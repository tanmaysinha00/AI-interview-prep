import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getInterviews, createInterview, type InterviewResponse, type DifficultyLevel } from '../api/interviews'
import Navbar from '../components/Navbar'

const TOPIC_OPTIONS = [
  'spring-boot', 'java-concurrency', 'system-design', 'databases',
  'data-structures', 'algorithms', 'microservices', 'kafka', 'redis',
  'kubernetes', 'rest-api', 'security', 'react', 'typescript',
]

const DIFFICULTY_OPTIONS: DifficultyLevel[] = ['EASY', 'MEDIUM', 'HARD', 'EXPERT']

export default function DashboardPage() {
  const [interviews, setInterviews] = useState<InterviewResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [selectedTopics, setSelectedTopics] = useState<string[]>(['spring-boot'])
  const [questionCount, setQuestionCount] = useState(5)
  const [difficulty, setDifficulty] = useState<DifficultyLevel>('MEDIUM')
  const [yearsOfExperience, setYearsOfExperience] = useState(2)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    getInterviews()
      .then((res) => setInterviews(res.data))
      .catch(() => setError('Failed to load interviews'))
      .finally(() => setLoading(false))
  }, [])

  const toggleTopic = (t: string) => {
    setSelectedTopics((prev) =>
      prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]
    )
  }

  const handleCreate = async () => {
    if (selectedTopics.length === 0) { setError('Select at least one topic'); return }
    setError('')
    setCreating(true)
    try {
      const res = await createInterview(selectedTopics, questionCount, difficulty, yearsOfExperience)
      navigate(`/interview/${res.data.interviewId}`)
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      setError(detail ?? 'Failed to start interview')
      setCreating(false)
    }
  }

  const statusBadge = (status: string) => {
    const colours: Record<string, string> = {
      IN_PROGRESS: 'bg-yellow-900/50 text-yellow-300 border-yellow-700',
      COMPLETED:   'bg-green-900/50 text-green-300 border-green-700',
      ABANDONED:   'bg-gray-800 text-gray-400 border-gray-700',
      CREATED:     'bg-blue-900/50 text-blue-300 border-blue-700',
    }
    return colours[status] ?? 'bg-gray-800 text-gray-400 border-gray-700'
  }

  return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />
      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-white text-2xl font-semibold">Dashboard</h1>
          <button
            onClick={() => setShowForm(!showForm)}
            className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            {showForm ? 'Cancel' : '+ New Interview'}
          </button>
        </div>

        {error && (
          <div className="bg-red-900/40 border border-red-700 text-red-300 text-sm px-4 py-2 rounded-lg mb-4">
            {error}
          </div>
        )}

        {showForm && (
          <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 mb-6">
            <h2 className="text-white font-medium mb-4">Configure Interview</h2>

            <div className="mb-4">
              <label className="text-gray-400 text-sm block mb-2">Topics</label>
              <div className="flex flex-wrap gap-2">
                {TOPIC_OPTIONS.map((t) => (
                  <button
                    key={t}
                    onClick={() => toggleTopic(t)}
                    className={`px-3 py-1 rounded-full text-xs border transition-colors ${
                      selectedTopics.includes(t)
                        ? 'bg-indigo-600 border-indigo-500 text-white'
                        : 'bg-gray-800 border-gray-600 text-gray-400 hover:border-gray-500'
                    }`}
                  >
                    {t}
                  </button>
                ))}
              </div>
            </div>

            <div className="mb-4">
              <label className="text-gray-400 text-sm block mb-1.5">Years of Experience</label>
              <div className="flex items-center gap-3">
                <input
                  type="range"
                  min={0}
                  max={15}
                  value={yearsOfExperience}
                  onChange={(e) => setYearsOfExperience(Number(e.target.value))}
                  className="w-48 accent-indigo-500"
                />
                <span className="text-white text-sm w-24">
                  {yearsOfExperience} yr{yearsOfExperience !== 1 ? 's' : ''}&nbsp;
                  <span className="text-gray-500 text-xs">
                    ({yearsOfExperience <= 1 ? 'Junior' : yearsOfExperience <= 3 ? 'Mid-level' : yearsOfExperience <= 6 ? 'Senior' : 'Staff+'})
                  </span>
                </span>
              </div>
            </div>

            <div className="flex gap-6 mb-5">
              <div>
                <label className="text-gray-400 text-sm block mb-1.5">Questions</label>
                <select
                  value={questionCount}
                  onChange={(e) => setQuestionCount(Number(e.target.value))}
                  className="bg-gray-800 border border-gray-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  {[3, 5, 8, 10, 15, 20].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-gray-400 text-sm block mb-1.5">Starting Difficulty</label>
                <select
                  value={difficulty}
                  onChange={(e) => setDifficulty(e.target.value as DifficultyLevel)}
                  className="bg-gray-800 border border-gray-700 text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  {DIFFICULTY_OPTIONS.map((d) => (
                    <option key={d} value={d}>{d}</option>
                  ))}
                </select>
              </div>
            </div>

            <button
              onClick={handleCreate}
              disabled={creating}
              className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white px-5 py-2 rounded-lg text-sm font-medium transition-colors"
            >
              {creating ? 'Starting…' : 'Start Interview'}
            </button>
          </div>
        )}

        {loading ? (
          <p className="text-gray-500 text-sm">Loading…</p>
        ) : interviews.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-gray-500 mb-2">No interviews yet.</p>
            <p className="text-gray-600 text-sm">Click "New Interview" to start one.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {interviews.map((iv) => (
              <div
                key={iv.interviewId}
                className="bg-gray-900 border border-gray-700 rounded-xl px-5 py-4 flex items-center justify-between"
              >
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`text-xs border px-2 py-0.5 rounded-full ${statusBadge(iv.status)}`}>
                      {iv.status.replace('_', ' ')}
                    </span>
                    <span className="text-gray-500 text-xs">
                      {new Date(iv.startedAt).toLocaleDateString()}
                    </span>
                  </div>
                  <p className="text-gray-300 text-sm">{iv.topics.join(', ')}</p>
                  <p className="text-gray-500 text-xs mt-0.5">
                    {iv.config.questionCount} questions · {iv.config.currentDifficulty}
                    {iv.totalScore != null && ` · Score: ${iv.totalScore}/10`}
                  </p>
                </div>
                <div className="flex gap-2">
                  {iv.status === 'IN_PROGRESS' && (
                    <button
                      onClick={() => navigate(`/interview/${iv.interviewId}`)}
                      className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs px-3 py-1.5 rounded-lg transition-colors"
                    >
                      Continue
                    </button>
                  )}
                  {iv.status === 'COMPLETED' && (
                    <button
                      onClick={() => navigate(`/interview/${iv.interviewId}/summary`)}
                      className="bg-gray-700 hover:bg-gray-600 text-white text-xs px-3 py-1.5 rounded-lg transition-colors"
                    >
                      Summary
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
