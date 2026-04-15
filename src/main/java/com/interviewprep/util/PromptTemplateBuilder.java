package com.interviewprep.util;

import com.interviewprep.entity.DifficultyLevel;
import com.interviewprep.entity.QuestionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles Claude API prompts for question generation, evaluation, and interview summary.
 * All system prompts enforce JSON-only output — no markdown fences or prose.
 */
@Component
public class PromptTemplateBuilder {

    private static final String QUESTION_SCHEMA = """
            {
              "title": "string — short title for the question card header (max 80 chars)",
              "body": "string — full question text with all necessary context (max 600 chars)",
              "codeSnippet": "string or null — Java/config/SQL code block if relevant (max 800 chars), else null",
              "hints": ["string — max 120 chars each, max 3 hints"],
              "options": ["string — max 150 chars each"] or null,
              "timeEstimateSeconds": number,
              "conceptsTested": ["string"],
              "relatedTopics": ["string"]
            }""";

    private static final String EVALUATION_SCHEMA = """
            {
              "score": number between 0 and 10,
              "maxScore": 10,
              "verdict": "CORRECT" or "PARTIALLY_CORRECT" or "INCORRECT",
              "feedback": {
                "summary": "string — one-line verdict",
                "detailed": "string — paragraph explanation",
                "correctApproach": "string — model answer or ideal approach",
                "commonMistakes": ["string"],
                "followUpSuggestion": "string — what to study next"
              },
              "difficultyAdjustment": "STAY" or "INCREASE" or "DECREASE"
            }""";

    private static final String SUMMARY_SCHEMA = """
            {
              "strengths": ["string"],
              "weaknesses": ["string"],
              "studyPlan": "string — 2-3 paragraph personalised study plan"
            }""";

    // ----------------------------------------------------------------
    // Question generation
    // ----------------------------------------------------------------

    public String buildQuestionSystemPrompt() {
        return """
                You are a senior technical interviewer at a top-tier software engineering company.
                Your role is to generate challenging, realistic interview questions.

                CRITICAL OUTPUT REQUIREMENT: Respond ONLY with valid JSON matching the schema below.
                Do NOT include markdown code fences, backticks, explanatory prose, greetings, or any \
                text outside the JSON object. The very first character of your response must be '{' \
                and the last must be '}'.

                Schema:
                """ + QUESTION_SCHEMA;
    }

    public String buildQuestionPrompt(
            String topic,
            DifficultyLevel difficulty,
            QuestionType type,
            List<String> previousTopics,
            int yearsOfExperience
    ) {
        StringBuilder sb = new StringBuilder();

        // Describe the candidate's experience level so Claude calibrates depth and terminology
        String experienceLabel = experienceLabel(yearsOfExperience);
        sb.append("Candidate profile: ").append(yearsOfExperience).append(" year(s) of experience (")
          .append(experienceLabel).append(").\n");
        sb.append("Calibrate language complexity, assumed knowledge, and depth to this experience level.\n\n");

        sb.append("Generate a ").append(difficulty.name()).append(" difficulty ")
          .append(formatType(type)).append(" question on the topic: ").append(topic).append(".\n\n");

        if (type == QuestionType.MCQ) {
            sb.append("Include exactly 4 options in the 'options' array. Only one option should be correct.\n\n");
        } else {
            sb.append("Set 'options' to null.\n\n");
        }

        if (!previousTopics.isEmpty()) {
            sb.append("Previously covered topics in this interview (avoid repeating the same specific concept): ")
              .append(String.join(", ", previousTopics)).append(".\n\n");
        }

        sb.append("For timeEstimateSeconds: EASY=120, MEDIUM=300, HARD=480, EXPERT=600.\n\n");
        sb.append("Length constraints (strict):\n");
        sb.append("- body: max 600 characters\n");
        sb.append("- codeSnippet: max 800 characters or null\n");
        sb.append("- each hint: max 120 characters, max 3 hints\n");
        sb.append("- each MCQ option: max 150 characters\n");
        sb.append("Candidates answer in a text box with a 2000 character limit — keep questions concise and focused.");

        return sb.toString();
    }

    private String experienceLabel(int years) {
        if (years <= 1) return "junior / entry-level";
        if (years <= 3) return "mid-level";
        if (years <= 6) return "senior";
        if (years <= 10) return "staff / principal";
        return "distinguished / architect";
    }

    // ----------------------------------------------------------------
    // Answer evaluation
    // ----------------------------------------------------------------

    public String buildEvaluationSystemPrompt() {
        return """
                You are evaluating a candidate's answer to a technical interview question.
                Score objectively and provide constructive, specific feedback.

                CRITICAL OUTPUT REQUIREMENT: Respond ONLY with valid JSON matching the schema below.
                Do NOT include markdown code fences, backticks, explanatory prose, or any text outside \
                the JSON object. The very first character of your response must be '{' and the last must be '}'.

                Schema:
                """ + EVALUATION_SCHEMA;
    }

    public String buildEvaluationPrompt(
            String questionTitle,
            String questionBody,
            String codeSnippet,
            DifficultyLevel difficulty,
            QuestionType questionType,
            List<String> options,
            String candidateAnswer
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Question type: ").append(questionType.name()).append("\n");
        sb.append("Question (").append(difficulty.name()).append(" difficulty):\n");
        sb.append("Title: ").append(questionTitle).append("\n");
        sb.append("Body: ").append(questionBody).append("\n");

        if (codeSnippet != null && !codeSnippet.isBlank()) {
            sb.append("Code:\n").append(codeSnippet).append("\n");
        }

        if (questionType == QuestionType.MCQ && options != null && !options.isEmpty()) {
            sb.append("\nAnswer options:\n");
            for (int i = 0; i < options.size(); i++) {
                sb.append("  ").append((char) ('A' + i)).append(") ").append(options.get(i)).append("\n");
            }
            sb.append("\nCandidate selected: ").append(candidateAnswer).append("\n\n");
            sb.append("""
                    This is a MULTIPLE CHOICE question. Evaluation rules:
                    - Determine which option is the single correct answer based on your technical knowledge.
                    - If the candidate selected the correct option: verdict = "CORRECT", score = 10.
                    - If the candidate selected a wrong option: verdict = "INCORRECT", score = 0.
                    - NEVER use "PARTIALLY_CORRECT" for MCQ questions.
                    - In 'correctApproach', state clearly which option was correct and why.
                    - In 'detailed', briefly explain why the selected option is correct or incorrect.
                    - For difficultyAdjustment: INCREASE if CORRECT, DECREASE if INCORRECT.""");
        } else {
            sb.append("\nCandidate's Answer:\n").append(candidateAnswer).append("\n\n");
            sb.append("""
                    Scoring rubric:
                    - 9-10: Complete, accurate, shows deep understanding and best practices
                    - 7-8: Mostly correct with minor gaps
                    - 5-6: Partially correct, missing key concepts
                    - 3-4: Some relevant knowledge but significant gaps
                    - 1-2: Mostly incorrect but shows some awareness
                    - 0: Completely incorrect or no answer

                    For difficultyAdjustment: INCREASE if score >= 8, DECREASE if score <= 4, else STAY.""");
        }

        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Interview summary
    // ----------------------------------------------------------------

    public String buildSummarySystemPrompt() {
        return """
                You are generating an interview performance summary and personalised study plan.

                CRITICAL OUTPUT REQUIREMENT: Respond ONLY with valid JSON matching the schema below.
                Do NOT include markdown code fences, backticks, explanatory prose, or any text outside \
                the JSON object. The very first character of your response must be '{' and the last must be '}'.

                Schema:
                """ + SUMMARY_SCHEMA;
    }

    public String buildSummaryPrompt(
            List<String> topics,
            double overallScore,
            List<String> questionSummaries
    ) {
        return "Interview topics: " + String.join(", ", topics) + "\n" +
               "Overall score: " + overallScore + "/10\n\n" +
               "Question and answer performance:\n" +
               String.join("\n", questionSummaries) + "\n\n" +
               "Based on the above performance, identify 2-4 strengths, 2-4 improvement areas, " +
               "and write a personalised 2-3 paragraph study plan.";
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String formatType(QuestionType type) {
        return switch (type) {
            case MCQ            -> "multiple-choice";
            case SHORT_ANSWER   -> "short-answer";
            case SCENARIO       -> "scenario-based";
            case CODE_REVIEW    -> "code review";
            case HANDS_ON_CODING -> "hands-on coding";
            case SYSTEM_DESIGN  -> "system design";
        };
    }
}
