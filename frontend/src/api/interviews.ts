import api from './client'

export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD' | 'EXPERT'
export type QuestionType = 'MCQ' | 'SHORT_ANSWER' | 'SCENARIO' | 'CODE_REVIEW' | 'HANDS_ON_CODING' | 'SYSTEM_DESIGN'
export type InterviewStatus = 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'

export interface InterviewConfig {
  questionCount: number
  initialDifficulty: DifficultyLevel
  currentDifficulty: DifficultyLevel
}

export interface InterviewResponse {
  interviewId: string
  status: InterviewStatus
  topics: string[]
  config: InterviewConfig
  totalScore: number | null
  startedAt: string
  completedAt: string | null
}

export interface QuestionDetail {
  title: string
  body: string
  codeSnippet: string | null
  hints: string[]
  options: string[] | null
  timeEstimateSeconds: number
}

export interface QuestionMetadata {
  conceptsTested: string[]
  relatedTopics: string[]
}

export interface QuestionResponse {
  questionId: string
  interviewId: string
  sequenceNumber: number
  topic: string
  difficulty: DifficultyLevel
  type: QuestionType
  question: QuestionDetail
  metadata: QuestionMetadata
}

export interface EvaluationFeedback {
  summary: string
  detailed: string
  correctApproach: string
  commonMistakes: string[]
  followUpSuggestion: string
}

export interface EvaluationResponse {
  questionId: string
  score: number
  maxScore: number
  verdict: 'CORRECT' | 'PARTIALLY_CORRECT' | 'INCORRECT'
  feedback: EvaluationFeedback
  difficultyAdjustment: 'STAY' | 'INCREASE' | 'DECREASE'
}

export interface TopicBreakdown {
  topic: string
  averageScore: number
  questionsAnswered: number
}

export interface DifficultyDataPoint {
  sequenceNumber: number
  difficulty: DifficultyLevel
}

export interface InterviewSummaryResponse {
  interviewId: string
  overallScore: number
  topicBreakdown: TopicBreakdown[]
  difficultyProgression: DifficultyDataPoint[]
  strengths: string[]
  weaknesses: string[]
  studyPlan: string
}

export const createInterview = (topics: string[], questionCount: number, difficulty: DifficultyLevel, yearsOfExperience: number) =>
  api.post<InterviewResponse>('/api/v1/interviews', { topics, questionCount, difficulty, yearsOfExperience })

export const getInterviews = () =>
  api.get<InterviewResponse[]>('/api/v1/interviews')

export const getNextQuestion = (interviewId: string) =>
  api.get<QuestionResponse>(`/api/v1/interviews/${interviewId}/next-question`)

export const submitAnswer = (interviewId: string, questionId: string, answer: string, timeTakenSeconds?: number) =>
  api.post<EvaluationResponse>(`/api/v1/interviews/${interviewId}/questions/${questionId}/answer`, {
    answer,
    timeTakenSeconds,
  })

export const getInterviewSummary = (interviewId: string) =>
  api.get<InterviewSummaryResponse>(`/api/v1/interviews/${interviewId}/summary`)
