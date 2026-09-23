package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AiTutorResponse(
    val concept: String,
    val whyItHappens: String? = null,
    val easyExplanation: String,
    val detailedExplanation: String,
    val realLifeExample: String? = null,
    val diagramExplanation: String? = null,
    val formulas: List<AiFormula>? = null,
    val commonMistakes: List<String>? = null,
    val boardExamTips: List<String>? = null,
    val jeeNeetTips: List<String>? = null,
    val quickRevision: String? = null,
    val flashcards: List<AiFlashcard>? = null,
    val quizItems: List<AiQuizItem>? = null,
    val stepByStepSolution: List<String>? = null,
    val followUpChips: List<String> = emptyList()
)

@Serializable
data class AiFormula(
    val formula: String,
    val description: String
)

@Serializable
data class AiFlashcard(
    val question: String,
    val answer: String,
    val hint: String? = null,
    val difficulty: String = "Medium"
)

@Serializable
data class AiQuizItem(
    val question: String,
    val options: List<String>? = null,
    val correctAnswer: String,
    val explanation: String,
    val type: String = "MCQ", // MCQ, Assertion Reason, Case Study, Competency, Numerical, Fill in blanks, True False
    val difficulty: String = "Medium" // Easy, Medium, Hard
)

enum class AiStudyMode(val label: String) {
    EXPLAIN("Explain"),
    TEACH_ME("Teach Me"),
    REVISION("Revision"),
    EXAM_MODE("Exam Mode"),
    QUIZ_MODE("Quiz Mode"),
    PRACTICE_MODE("Practice Mode"),
    DOUBT_MODE("Doubt Mode"),
    CRASH_COURSE("Crash Course")
}
