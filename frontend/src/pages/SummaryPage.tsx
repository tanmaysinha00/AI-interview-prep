import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getInterviewSummary, type InterviewSummaryResponse } from '../api/interviews'
import Navbar from '../components/Navbar'

export default function SummaryPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [summary, setSummary] = useState<InterviewSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getInterviewSummary(id!)
      .then((res) => setSummary(res.data))
      .catch(() => setError('Failed to load summary'))
      .finally(() => setLoading(false))
  }, [id])

  const scoreColour = (s: number) => {
    if (s >= 8) return 'text-green-400'
    if (s >= 5) return 'text-yellow-400'
    return 'text-red-400'
  }

  return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />
      <div className="max-w-3xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-white text-2xl font-semibold">Interview Summary</h1>
          <button
            onClick={() => navigate('/dashboard')}
            className="text-gray-400 hover:text-white text-sm transition-colors"
          >
            ← Dashboard
          </button>
        </div>

        {loading && <p className="text-gray-500 text-sm animate-pulse">Generating summary…</p>}
        {error && <p className="text-red-400 text-sm">{error}</p>}

        {summary && (
          <div className="space-y-5">
            {/* Score */}
            <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 text-center">
              <p className="text-gray-400 text-sm mb-1">Overall Score</p>
              <p className={`text-5xl font-bold ${scoreColour(summary.overallScore)}`}>
                {summary.overallScore.toFixed(1)}
                <span className="text-gray-600 text-2xl">/10</span>
              </p>
            </div>

            {/* Topic Breakdown */}
            <div className="bg-gray-900 border border-gray-700 rounded-xl p-6">
              <h2 className="text-white font-medium mb-4">Topic Breakdown</h2>
              <div className="space-y-3">
                {summary.topicBreakdown.map((t) => (
                  <div key={t.topic}>
                    <div className="flex justify-between text-sm mb-1">
                      <span className="text-gray-300">{t.topic}</span>
                      <span className={scoreColour(t.averageScore)}>
                        {t.averageScore.toFixed(1)}/10
                        <span className="text-gray-600 ml-2 text-xs">({t.questionsAnswered}q)</span>
                      </span>
                    </div>
                    <div className="h-1.5 bg-gray-800 rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          t.averageScore >= 8 ? 'bg-green-500' :
                          t.averageScore >= 5 ? 'bg-yellow-500' : 'bg-red-500'
                        }`}
                        style={{ width: `${(t.averageScore / 10) * 100}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Difficulty Progression */}
            {summary.difficultyProgression.length > 0 && (
              <div className="bg-gray-900 border border-gray-700 rounded-xl p-6">
                <h2 className="text-white font-medium mb-4">Difficulty Progression</h2>
                <div className="flex gap-2 flex-wrap">
                  {summary.difficultyProgression.map((d) => {
                    const colours: Record<string, string> = {
                      EASY: 'bg-green-900/40 border-green-700 text-green-400',
                      MEDIUM: 'bg-yellow-900/40 border-yellow-700 text-yellow-400',
                      HARD: 'bg-orange-900/40 border-orange-700 text-orange-400',
                      EXPERT: 'bg-red-900/40 border-red-700 text-red-400',
                    }
                    return (
                      <div key={d.sequenceNumber} className="text-center">
                        <div className={`border rounded-lg px-2 py-1 text-xs ${colours[d.difficulty] ?? ''}`}>
                          Q{d.sequenceNumber}
                        </div>
                        <div className="text-gray-600 text-xs mt-1">{d.difficulty[0]}</div>
                      </div>
                    )
                  })}
                </div>
              </div>
            )}

            {/* Strengths & Weaknesses */}
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-gray-900 border border-green-900/50 rounded-xl p-5">
                <h2 className="text-green-400 font-medium text-sm uppercase tracking-wide mb-3">Strengths</h2>
                <ul className="space-y-1.5">
                  {summary.strengths.map((s, i) => (
                    <li key={i} className="text-gray-300 text-sm flex gap-2">
                      <span className="text-green-500 mt-0.5">✓</span>
                      {s}
                    </li>
                  ))}
                </ul>
              </div>
              <div className="bg-gray-900 border border-yellow-900/50 rounded-xl p-5">
                <h2 className="text-yellow-400 font-medium text-sm uppercase tracking-wide mb-3">Areas to Improve</h2>
                <ul className="space-y-1.5">
                  {summary.weaknesses.map((w, i) => (
                    <li key={i} className="text-gray-300 text-sm flex gap-2">
                      <span className="text-yellow-500 mt-0.5">→</span>
                      {w}
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Study Plan */}
            <div className="bg-gray-900 border border-indigo-900/50 rounded-xl p-6">
              <h2 className="text-indigo-400 font-medium mb-3">Personalised Study Plan</h2>
              <p className="text-gray-300 text-sm leading-relaxed whitespace-pre-wrap">{summary.studyPlan}</p>
            </div>

            <div className="flex justify-center">
              <button
                onClick={() => navigate('/dashboard')}
                className="bg-indigo-600 hover:bg-indigo-500 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors"
              >
                Start Another Interview
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
