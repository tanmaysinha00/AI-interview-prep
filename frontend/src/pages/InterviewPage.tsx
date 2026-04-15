import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getNextQuestion,
  submitAnswer,
  type QuestionResponse,
  type EvaluationResponse,
} from '../api/interviews'
import Navbar from '../components/Navbar'

type PageState = 'loading' | 'question' | 'submitting' | 'evaluation' | 'done' | 'error'

const verdictColour = (v: string) => {
  if (v === 'CORRECT')           return 'text-green-400'
  if (v === 'PARTIALLY_CORRECT') return 'text-yellow-400'
  return 'text-red-400'
}

export default function InterviewPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [state, setState] = useState<PageState>('loading')
  const [question, setQuestion] = useState<QuestionResponse | null>(null)
  const [evaluation, setEvaluation] = useState<EvaluationResponse | null>(null)
  const [answer, setAnswer] = useState('')
  const [selectedOption, setSelectedOption] = useState<string | null>(null)
  const [error, setError] = useState('')
  const startTimeRef = useRef<number>(Date.now())

  const loadNextQuestion = async () => {
    setState('loading')
    setAnswer('')
    setSelectedOption(null)
    setEvaluation(null)
    setError('')
    try {
      const res = await getNextQuestion(id!)
      setQuestion(res.data)
      startTimeRef.current = Date.now()
      setState('question')
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      if (detail?.includes('All')) {
        setState('done')
      } else {
        setError(detail ?? 'Failed to load question')
        setState('error')
      }
    }
  }

  useEffect(() => { loadNextQuestion() }, [id])

  const handleSubmit = async () => {
    const finalAnswer = question?.type === 'MCQ' ? (selectedOption ?? '') : answer
    if (!finalAnswer.trim()) return
    const timeTaken = Math.round((Date.now() - startTimeRef.current) / 1000)
    setState('submitting')
    try {
      const res = await submitAnswer(id!, question!.questionId, finalAnswer, timeTaken)
      setEvaluation(res.data)
      setState('evaluation')
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      setError(detail ?? 'Failed to submit answer')
      setState('error')
    }
  }

  const isMCQ = question?.type === 'MCQ'

  return (
    <div className="min-h-screen bg-gray-950">
      <Navbar />
      <div className="max-w-3xl mx-auto px-4 py-8">

        {state === 'loading' && (
          <div className="text-center py-16">
            <div className="text-gray-400 text-sm animate-pulse">Generating question…</div>
          </div>
        )}

        {state === 'submitting' && (
          <div className="text-center py-16">
            <div className="text-gray-400 text-sm animate-pulse">Evaluating your answer…</div>
          </div>
        )}

        {state === 'error' && (
          <div className="bg-red-900/40 border border-red-700 text-red-300 px-4 py-3 rounded-lg text-sm">
            {error}
            <button onClick={loadNextQuestion} className="ml-3 underline text-red-200 hover:text-white">
              Try again
            </button>
          </div>
        )}

        {state === 'done' && (
          <div className="text-center py-16">
            <p className="text-white text-xl font-medium mb-2">Interview Complete</p>
            <p className="text-gray-400 text-sm mb-6">All questions answered.</p>
            <button
              onClick={() => navigate(`/interview/${id}/summary`)}
              className="bg-indigo-600 hover:bg-indigo-500 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors"
            >
              View Summary
            </button>
          </div>
        )}

        {(state === 'question' || state === 'evaluation') && question && (
          <>
            {/* Header */}
            <div className="flex items-center gap-2 mb-4">
              <span className="bg-indigo-900/50 border border-indigo-700 text-indigo-300 text-xs px-2.5 py-0.5 rounded-full">
                Q{question.sequenceNumber}
              </span>
              <span className="text-gray-500 text-xs uppercase tracking-wide">{question.topic}</span>
              <span className="text-gray-600 text-xs">·</span>
              <span className="text-gray-500 text-xs">{question.difficulty}</span>
              <span className="text-gray-600 text-xs">·</span>
              <span className="text-gray-500 text-xs">{question.type.replace('_', ' ')}</span>
            </div>

            {/* Question card */}
            <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 mb-4">
              <h2 className="text-white font-medium text-lg mb-3">{question.question.title}</h2>
              <p className="text-gray-300 text-sm leading-relaxed whitespace-pre-wrap mb-4">
                {question.question.body}
              </p>

              {question.question.codeSnippet && (
                <pre className="bg-gray-950 border border-gray-700 rounded-lg p-4 text-green-300 text-xs overflow-x-auto mb-4 leading-relaxed">
                  <code>{question.question.codeSnippet}</code>
                </pre>
              )}

              {question.question.hints.length > 0 && (
                <details className="mb-2">
                  <summary className="text-gray-500 text-xs cursor-pointer hover:text-gray-400">
                    Show hints ({question.question.hints.length})
                  </summary>
                  <ul className="mt-2 space-y-1">
                    {question.question.hints.map((h, i) => (
                      <li key={i} className="text-gray-400 text-xs pl-3 border-l border-gray-700">{h}</li>
                    ))}
                  </ul>
                </details>
              )}
            </div>

            {/* Answer section */}
            {state === 'question' && (
              <div className="space-y-3">
                {isMCQ && question.question.options ? (
                  <div className="space-y-2">
                    {question.question.options.map((opt, i) => (
                      <button
                        key={i}
                        onClick={() => setSelectedOption(opt)}
                        className={`w-full text-left px-4 py-3 rounded-lg border text-sm transition-colors ${
                          selectedOption === opt
                            ? 'bg-indigo-900/50 border-indigo-500 text-white'
                            : 'bg-gray-900 border-gray-700 text-gray-300 hover:border-gray-500'
                        }`}
                      >
                        {opt}
                      </button>
                    ))}
                  </div>
                ) : (
                  <div>
                    <textarea
                      value={answer}
                      onChange={(e) => setAnswer(e.target.value.slice(0, 2000))}
                      rows={7}
                      placeholder="Type your answer here…"
                      className="w-full bg-gray-900 border border-gray-700 text-white rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none placeholder-gray-600"
                    />
                    <div className="flex justify-end mt-1">
                      <span className={`text-xs ${answer.length >= 1900 ? 'text-yellow-400' : 'text-gray-600'}`}>
                        {answer.length}/2000
                      </span>
                    </div>
                  </div>
                )}
                <div className="flex justify-end">
                  <button
                    onClick={handleSubmit}
                    disabled={isMCQ ? !selectedOption : !answer.trim()}
                    className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors"
                  >
                    Submit Answer
                  </button>
                </div>
              </div>
            )}

            {/* Evaluation */}
            {state === 'evaluation' && evaluation && (
              <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <span className={`text-lg font-semibold ${verdictColour(evaluation.verdict)}`}>
                      {evaluation.verdict.replace('_', ' ')}
                    </span>
                    <span className="text-gray-400 text-sm ml-3">
                      {evaluation.score}/{evaluation.maxScore}
                    </span>
                  </div>
                  <span className={`text-xs border px-2.5 py-0.5 rounded-full ${
                    evaluation.difficultyAdjustment === 'INCREASE'
                      ? 'bg-green-900/40 border-green-700 text-green-400'
                      : evaluation.difficultyAdjustment === 'DECREASE'
                      ? 'bg-yellow-900/40 border-yellow-700 text-yellow-400'
                      : 'bg-gray-800 border-gray-600 text-gray-400'
                  }`}>
                    Difficulty: {evaluation.difficultyAdjustment}
                  </span>
                </div>

                <div>
                  <p className="text-white text-sm font-medium mb-1">{evaluation.feedback.summary}</p>
                  <p className="text-gray-400 text-sm leading-relaxed">{evaluation.feedback.detailed}</p>
                </div>

                <div>
                  <p className="text-gray-300 text-xs uppercase tracking-wide mb-1.5">Ideal Approach</p>
                  <p className="text-gray-400 text-sm leading-relaxed">{evaluation.feedback.correctApproach}</p>
                </div>

                {evaluation.feedback.commonMistakes.length > 0 && (
                  <div>
                    <p className="text-gray-300 text-xs uppercase tracking-wide mb-1.5">Common Mistakes</p>
                    <ul className="space-y-1">
                      {evaluation.feedback.commonMistakes.map((m, i) => (
                        <li key={i} className="text-gray-400 text-xs pl-3 border-l border-gray-700">{m}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {evaluation.feedback.followUpSuggestion && (
                  <div className="bg-indigo-950/40 border border-indigo-800 rounded-lg px-4 py-3">
                    <p className="text-indigo-300 text-xs font-medium mb-1">Study Next</p>
                    <p className="text-gray-300 text-sm">{evaluation.feedback.followUpSuggestion}</p>
                  </div>
                )}

                <div className="flex justify-end pt-1">
                  <button
                    onClick={loadNextQuestion}
                    className="bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-2 rounded-lg text-sm font-medium transition-colors"
                  >
                    Next Question →
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
