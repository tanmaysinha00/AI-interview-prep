#!/usr/bin/env bash
# ===========================================================
# setup-local.sh — Local development environment bootstrap
# Detects OS, installs missing prerequisites, validates setup
# ===========================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROPERTIES_FILE="$REPO_ROOT/src/main/resources/application-dev.properties"
TEMPLATE_FILE="$REPO_ROOT/src/main/resources/application-dev.properties.template"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}✅ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠️  $1${NC}"; }
err()  { echo -e "${RED}❌ $1${NC}"; }

# -----------------------------------------------------------
# Detect OS
# -----------------------------------------------------------
detect_os() {
    case "$(uname -s)" in
        Darwin) echo "macos" ;;
        Linux)
            if grep -qi microsoft /proc/version 2>/dev/null; then
                echo "wsl"
            else
                echo "linux"
            fi
            ;;
        *) echo "unknown" ;;
    esac
}

OS=$(detect_os)
echo ""
echo "============================================================"
echo " AI Interview Prep — Local Setup"
echo " Detected OS: $OS"
echo "============================================================"
echo ""

# -----------------------------------------------------------
# 1. Java 21
# -----------------------------------------------------------
check_java() {
    if command -v java &>/dev/null; then
        JAVA_VER=$(java --version 2>&1 | head -1 | awk '{print $2}' | cut -d. -f1)
        if [[ "$JAVA_VER" -ge 21 ]]; then
            ok "Java $JAVA_VER (meets requirement: 21+)"
            return 0
        else
            warn "Java $JAVA_VER found — need 21+"
        fi
    else
        warn "Java not found"
    fi
    return 1
}

install_java() {
    echo "Installing Java 21..."
    if command -v sdk &>/dev/null; then
        sdk install java 21.0.4-tem
    elif [[ "$OS" == "macos" ]]; then
        brew install openjdk@21
        echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> "$HOME/.zshrc"
        export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
    elif [[ "$OS" == "linux" || "$OS" == "wsl" ]]; then
        sudo apt-get update -q
        sudo apt-get install -y openjdk-21-jdk
    else
        err "Cannot auto-install Java on $OS — please install Java 21 manually."
        exit 1
    fi
}

check_java || install_java
check_java || { err "Java 21 installation failed"; exit 1; }

# -----------------------------------------------------------
# 2. Maven (prefer ./mvnw)
# -----------------------------------------------------------
check_maven() {
    if [[ -f "$REPO_ROOT/mvnw" ]]; then
        ok "Maven Wrapper (./mvnw) present — will use this"
        return 0
    fi
    if command -v mvn &>/dev/null; then
        MVN_VER=$(mvn --version 2>&1 | head -1 | awk '{print $3}')
        ok "Maven $MVN_VER (./mvnw preferred in CI and docs)"
        return 0
    fi
    warn "Neither ./mvnw nor mvn found"
    return 1
}

check_maven || {
    warn "No Maven Wrapper found — the project requires ./mvnw. Check if you cloned the full repo."
    exit 1
}

# -----------------------------------------------------------
# 3. Node.js 20 LTS
# -----------------------------------------------------------
check_node() {
    if command -v node &>/dev/null; then
        NODE_MAJOR=$(node --version | tr -d 'v' | cut -d. -f1)
        if [[ "$NODE_MAJOR" -ge 20 ]]; then
            ok "Node.js $(node --version)"
            return 0
        else
            warn "Node.js $(node --version) found — need 20 LTS+"
        fi
    else
        warn "Node.js not found"
    fi
    return 1
}

install_node() {
    echo "Installing Node.js 20 LTS..."
    if command -v nvm &>/dev/null; then
        nvm install 20
        nvm use 20
    elif [[ "$OS" == "macos" ]]; then
        brew install node@20
        echo 'export PATH="/opt/homebrew/opt/node@20/bin:$PATH"' >> "$HOME/.zshrc"
        export PATH="/opt/homebrew/opt/node@20/bin:$PATH"
    elif [[ "$OS" == "linux" || "$OS" == "wsl" ]]; then
        curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
        sudo apt-get install -y nodejs
    else
        err "Cannot auto-install Node.js on $OS — please install Node 20 LTS manually."
        exit 1
    fi
}

check_node || install_node
check_node || { err "Node.js 20 installation failed"; exit 1; }

# -----------------------------------------------------------
# 4. Docker check — offer Docker Compose route for PG + Redis
# -----------------------------------------------------------
USE_DOCKER=false
if command -v docker &>/dev/null && docker info &>/dev/null 2>&1; then
    ok "Docker detected"
    echo ""
    read -rp "Use Docker Compose for PostgreSQL + Redis instead of local installs? [Y/n]: " use_docker_choice
    use_docker_choice="${use_docker_choice:-Y}"
    if [[ "$use_docker_choice" =~ ^[Yy]$ ]]; then
        USE_DOCKER=true
    fi
fi

# -----------------------------------------------------------
# 5. PostgreSQL 16
# -----------------------------------------------------------
check_postgres() {
    if command -v pg_isready &>/dev/null; then
        if pg_isready -h localhost -p 5432 &>/dev/null; then
            ok "PostgreSQL running on localhost:5432"
            return 0
        else
            warn "pg_isready found but PostgreSQL not running on localhost:5432"
        fi
    else
        warn "PostgreSQL not found"
    fi
    return 1
}

install_postgres() {
    echo "Installing PostgreSQL 16..."
    if [[ "$OS" == "macos" ]]; then
        brew install postgresql@16
        brew services start postgresql@16
        echo 'export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"' >> "$HOME/.zshrc"
        export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"
    elif [[ "$OS" == "linux" || "$OS" == "wsl" ]]; then
        sudo apt-get update -q
        sudo apt-get install -y postgresql-16
        sudo systemctl start postgresql
    else
        err "Cannot auto-install PostgreSQL on $OS"
        exit 1
    fi
    # Create dev database and user
    sleep 2
    createdb interviewprep_dev 2>/dev/null || true
    psql -c "CREATE USER dev WITH PASSWORD 'devpass';" 2>/dev/null || true
    psql -c "GRANT ALL PRIVILEGES ON DATABASE interviewprep_dev TO dev;" 2>/dev/null || true
}

# -----------------------------------------------------------
# 6. Redis 7
# -----------------------------------------------------------
check_redis() {
    if command -v redis-cli &>/dev/null; then
        if redis-cli -h localhost -p 6379 ping &>/dev/null; then
            ok "Redis running on localhost:6379"
            return 0
        else
            warn "redis-cli found but Redis not running on localhost:6379"
        fi
    else
        warn "Redis not found"
    fi
    return 1
}

install_redis() {
    echo "Installing Redis 7..."
    if [[ "$OS" == "macos" ]]; then
        brew install redis
        brew services start redis
    elif [[ "$OS" == "linux" || "$OS" == "wsl" ]]; then
        sudo apt-get update -q
        sudo apt-get install -y redis-server
        sudo systemctl start redis-server
    else
        err "Cannot auto-install Redis on $OS"
        exit 1
    fi
}

if [[ "$USE_DOCKER" == "true" ]]; then
    echo ""
    echo "Starting PostgreSQL + Redis via Docker Compose..."
    cd "$REPO_ROOT"
    docker compose -f docker-compose.dev.yml up -d
    echo "Waiting for containers to be healthy..."
    sleep 5
    ok "Docker Compose services started"
else
    check_postgres || install_postgres
    check_postgres || { err "PostgreSQL not available — check installation"; exit 1; }
    check_redis || install_redis
    check_redis || { err "Redis not available — check installation"; exit 1; }
fi

# -----------------------------------------------------------
# 7. Create application-dev.properties from template
# -----------------------------------------------------------
echo ""
if [[ -f "$PROPERTIES_FILE" ]]; then
    warn "application-dev.properties already exists — skipping template copy"
else
    cp "$TEMPLATE_FILE" "$PROPERTIES_FILE"
    ok "Created application-dev.properties from template"

    echo ""
    read -rp "Paste your Claude API key (sk-ant-...): " claude_key
    if [[ -n "$claude_key" ]]; then
        sed -i.bak "s|REPLACE_WITH_YOUR_CLAUDE_API_KEY|$claude_key|g" "$PROPERTIES_FILE"
        rm -f "${PROPERTIES_FILE}.bak"
        ok "Claude API key written to application-dev.properties"
    else
        warn "No API key entered — update app.claude.api-key in application-dev.properties manually"
    fi

    # Generate a random JWT secret
    JWT_SECRET=$(openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | base64 | tr -d '=\n/')
    sed -i.bak "s|REPLACE_WITH_YOUR_JWT_SECRET_MIN_256_BITS|$JWT_SECRET|g" "$PROPERTIES_FILE"
    rm -f "${PROPERTIES_FILE}.bak"
    ok "JWT secret auto-generated and written to application-dev.properties"
fi

# -----------------------------------------------------------
# 8. Validation
# -----------------------------------------------------------
echo ""
echo "============================================================"
echo " Running validation checks..."
echo "============================================================"

# Maven validate
cd "$REPO_ROOT"
if [[ -f "./mvnw" ]]; then
    MAVEN_CMD="./mvnw"
else
    MAVEN_CMD="mvn"
fi

if $MAVEN_CMD validate -q 2>/dev/null; then
    JAVA_VER_FULL=$(java --version 2>&1 | head -1 | awk '{print $2}')
    ok "Java $JAVA_VER_FULL"
    ok "Maven Wrapper (./mvnw)"
    ok "Maven project structure valid"
else
    warn "Maven validate failed — check pom.xml"
fi

# PostgreSQL
if [[ "$USE_DOCKER" == "true" ]]; then
    if docker compose -f docker-compose.dev.yml exec -T postgres pg_isready -U dev -d interviewprep_dev &>/dev/null; then
        ok "PostgreSQL running (Docker)"
    else
        warn "PostgreSQL container not ready yet — try again in a few seconds"
    fi
elif command -v pg_isready &>/dev/null && pg_isready -h localhost -p 5432 &>/dev/null; then
    ok "PostgreSQL running (local)"
else
    warn "PostgreSQL not responding — check service status"
fi

# Redis
if [[ "$USE_DOCKER" == "true" ]]; then
    if docker compose -f docker-compose.dev.yml exec -T redis redis-cli ping 2>/dev/null | grep -q PONG; then
        ok "Redis running (Docker)"
    else
        warn "Redis container not ready yet"
    fi
elif command -v redis-cli &>/dev/null && redis-cli -h localhost -p 6379 ping 2>/dev/null | grep -q PONG; then
    ok "Redis running (local)"
else
    warn "Redis not responding — check service status"
fi

# Config file
if [[ -f "$PROPERTIES_FILE" ]]; then
    ok "application-dev.properties created"
else
    err "application-dev.properties missing"
fi

echo ""
echo "============================================================"
echo " Setup complete!"
echo ""
echo " Start the backend:"
echo "   $MAVEN_CMD spring-boot:run -Dspring-boot.run.profiles=dev"
echo ""
echo " Start the frontend:"
echo "   cd frontend && npm run dev"
echo "============================================================"
echo ""
