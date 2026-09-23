package com.example.data.db

import com.example.data.model.ExamEntity
import com.example.data.model.NoteEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.StudentProfile
import com.example.data.model.StudyTargetEntity
import com.example.data.model.SyllabusChapterEntity

object DefaultData {
    val defaultProfile = StudentProfile(
        id = 1,
        name = "",
        studentClass = "Class 12",
        board = "CBSE",
        stream = "PCM",
        subjects = "Physics, Chemistry, Mathematics",
        targetPercentage = 95,
        dailyStudyGoalMinutes = 180,
        languagePreference = "Hinglish",
        currentPrepLevel = "Intermediate",
        streakDays = 0,
        lastStudyDate = "",
        isOnboarded = true
    )

    val initialExams = listOf(
        ExamEntity(
            id = 1,
            examName = "CBSE Pre-Board Exam",
            subject = "Physics & Chemistry",
            examDate = "2026-10-18",
            targetScore = 95,
            syllabusCoverage = "Ch 1 to Ch 5 (Term 1)",
            isActive = true
        )
    )

    val initialChapters = listOf(
        // PHYSICS - Class 12 CBSE
        SyllabusChapterEntity(
            id = "PHY_01",
            subject = "Physics",
            chapterNumber = 1,
            title = "Electric Charges and Fields",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = true,
            pyqDone = true,
            revisionDone = true,
            testDone = true
        ),
        SyllabusChapterEntity(
            id = "PHY_02",
            subject = "Physics",
            chapterNumber = 2,
            title = "Electrostatic Potential and Capacitance",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = true,
            pyqDone = false,
            revisionDone = false,
            testDone = false,
            isWeakTopic = true
        ),
        SyllabusChapterEntity(
            id = "PHY_03",
            subject = "Physics",
            chapterNumber = 3,
            title = "Current Electricity",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = true,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_04",
            subject = "Physics",
            chapterNumber = 4,
            title = "Moving Charges and Magnetism",
            conceptsDone = true,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_05",
            subject = "Physics",
            chapterNumber = 5,
            title = "Magnetism and Matter",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_06",
            subject = "Physics",
            chapterNumber = 6,
            title = "Electromagnetic Induction",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_07",
            subject = "Physics",
            chapterNumber = 7,
            title = "Alternating Current",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_08",
            subject = "Physics",
            chapterNumber = 8,
            title = "Electromagnetic Waves",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_09",
            subject = "Physics",
            chapterNumber = 9,
            title = "Ray Optics and Optical Instruments",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_10",
            subject = "Physics",
            chapterNumber = 10,
            title = "Wave Optics",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_11",
            subject = "Physics",
            chapterNumber = 11,
            title = "Dual Nature of Radiation and Matter",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_12",
            subject = "Physics",
            chapterNumber = 12,
            title = "Atoms",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_13",
            subject = "Physics",
            chapterNumber = 13,
            title = "Nuclei",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "PHY_14",
            subject = "Physics",
            chapterNumber = 14,
            title = "Semiconductor Electronics",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),

        // CHEMISTRY - Class 12 CBSE
        SyllabusChapterEntity(
            id = "CHEM_01",
            subject = "Chemistry",
            chapterNumber = 1,
            title = "Solutions",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = true,
            pyqDone = true,
            revisionDone = true,
            testDone = true
        ),
        SyllabusChapterEntity(
            id = "CHEM_02",
            subject = "Chemistry",
            chapterNumber = 2,
            title = "Electrochemistry",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false,
            isWeakTopic = true
        ),
        SyllabusChapterEntity(
            id = "CHEM_03",
            subject = "Chemistry",
            chapterNumber = 3,
            title = "Chemical Kinetics",
            conceptsDone = true,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "CHEM_04",
            subject = "Chemistry",
            chapterNumber = 4,
            title = "d- and f-Block Elements",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "CHEM_05",
            subject = "Chemistry",
            chapterNumber = 5,
            title = "Coordination Compounds",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "CHEM_06",
            subject = "Chemistry",
            chapterNumber = 6,
            title = "Haloalkanes and Haloarenes",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "CHEM_07",
            subject = "Chemistry",
            chapterNumber = 7,
            title = "Alcohols, Phenols and Ethers",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "CHEM_08",
            subject = "Chemistry",
            chapterNumber = 8,
            title = "Aldehydes, Ketones and Carboxylic Acids",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "CHEM_09",
            subject = "Chemistry",
            chapterNumber = 9,
            title = "Amines",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "CHEM_10",
            subject = "Chemistry",
            chapterNumber = 10,
            title = "Biomolecules",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),

        // MATHEMATICS - Class 12 CBSE
        SyllabusChapterEntity(
            id = "MATH_01",
            subject = "Mathematics",
            chapterNumber = 1,
            title = "Relations and Functions",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = true,
            pyqDone = true,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_02",
            subject = "Mathematics",
            chapterNumber = 2,
            title = "Inverse Trigonometric Functions",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = true,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_03",
            subject = "Mathematics",
            chapterNumber = 3,
            title = "Matrices",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = true,
            pyqDone = true,
            revisionDone = true,
            testDone = true
        ),
        SyllabusChapterEntity(
            id = "MATH_04",
            subject = "Mathematics",
            chapterNumber = 4,
            title = "Determinants",
            conceptsDone = true,
            ncertReadingDone = true,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_05",
            subject = "Mathematics",
            chapterNumber = 5,
            title = "Continuity and Differentiability",
            conceptsDone = true,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_06",
            subject = "Mathematics",
            chapterNumber = 6,
            title = "Application of Derivatives",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_07",
            subject = "Mathematics",
            chapterNumber = 7,
            title = "Integrals",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_08",
            subject = "Mathematics",
            chapterNumber = 8,
            title = "Application of Integrals",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_09",
            subject = "Mathematics",
            chapterNumber = 9,
            title = "Differential Equations",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_10",
            subject = "Mathematics",
            chapterNumber = 10,
            title = "Vector Algebra",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_11",
            subject = "Mathematics",
            chapterNumber = 11,
            title = "Three Dimensional Geometry",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_12",
            subject = "Mathematics",
            chapterNumber = 12,
            title = "Linear Programming",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        ),
        SyllabusChapterEntity(
            id = "MATH_13",
            subject = "Mathematics",
            chapterNumber = 13,
            title = "Probability",
            conceptsDone = false,
            ncertReadingDone = false,
            ncertQuestionsDone = false,
            pyqDone = false,
            revisionDone = false,
            testDone = false
        )
    )

    val initialQuestions = listOf(
        QuestionEntity(
            id = 1,
            subject = "Physics",
            chapter = "Current Electricity",
            topic = "Drift Velocity & Ohm's Law",
            difficulty = "Medium",
            questionType = "MCQ",
            questionText = "If temperature of a metallic conductor is increased, what happens to the relaxation time (τ) of free electrons and the drift velocity (vd)?",
            optionsListJson = "τ increases, vd decreases|τ decreases, vd decreases|τ increases, vd increases|Both remain unchanged",
            correctAnswer = "τ decreases, vd decreases",
            hint = "As temperature increases, electrons collide more frequently with vibrating positive ions in the lattice.",
            solutionExplanation = "With rise in temperature, the thermal agitation of lattice ions increases. Free electrons collide more rapidly, reducing relaxation time τ. Since vd = (e * E * τ) / m, decreasing τ directly decreases the drift velocity vd.",
            isBookmarked = true
        ),
        QuestionEntity(
            id = 2,
            subject = "Physics",
            chapter = "Current Electricity",
            topic = "Kirchhoff's Laws",
            difficulty = "Hard",
            questionType = "MCQ",
            questionText = "Kirchhoff's First Rule (Junction rule) and Second Rule (Loop rule) are based on the conservation laws of:",
            optionsListJson = "Energy and Momentum|Charge and Energy|Charge and Mass|Momentum and Energy",
            correctAnswer = "Charge and Energy",
            hint = "Total charge entering a node equals charge leaving. In a closed loop, the total work done moving charge across potential drops is zero.",
            solutionExplanation = "Junction Rule (ΣI = 0) is based on conservation of electric charge. Loop Rule (ΣΔV = 0) represents conservation of electric energy in an electrostatic field.",
            isBookmarked = false
        ),
        QuestionEntity(
            id = 3,
            subject = "Physics",
            chapter = "Electrostatic Potential and Capacitance",
            topic = "Capacitance with Dielectric",
            difficulty = "Medium",
            questionType = "MCQ",
            questionText = "A parallel plate capacitor is charged by a battery and then disconnected. A dielectric slab of dielectric constant K is now inserted. The energy stored in the capacitor:",
            optionsListJson = "Increases by factor K|Decreases by factor 1/K|Remains constant|Increases by factor K^2",
            correctAnswer = "Decreases by factor 1/K",
            hint = "Since battery is disconnected, charge Q remains constant. Use U = Q^2 / (2C).",
            solutionExplanation = "Charge Q = const. Capacitance becomes C' = K * C. New energy U' = Q^2 / (2 * K * C) = U / K. Therefore energy decreases by factor 1/K.",
            isBookmarked = true
        ),
        QuestionEntity(
            id = 4,
            subject = "Chemistry",
            chapter = "Electrochemistry",
            topic = "Nernst Equation",
            difficulty = "Hard",
            questionType = "MCQ",
            questionText = "For the cell reaction Zn(s) + Cu²⁺(aq) -> Zn²⁺(aq) + Cu(s), E°cell is 1.10 V. If concentration of Zn²⁺ is increased 10 times at 298 K, what happens to Ecell?",
            optionsListJson = "Increases by 0.0295 V|Decreases by 0.0295 V|Decreases by 0.059 V|Remains unchanged",
            correctAnswer = "Decreases by 0.0295 V",
            hint = "Use Nernst Equation: E = E° - (0.0591 / n) * log([Zn²⁺]/[Cu²⁺]) with n = 2.",
            solutionExplanation = "E = E° - (0.0591 / 2) * log([Zn²⁺]/[Cu²⁺]). When [Zn²⁺] is increased 10-fold, log term increases by log(10) = 1. Hence E decreases by (0.0591 / 2) ≈ 0.0295 V.",
            isBookmarked = true
        ),
        QuestionEntity(
            id = 5,
            subject = "Chemistry",
            chapter = "Solutions",
            topic = "Colligative Properties & Van't Hoff Factor",
            difficulty = "Medium",
            questionType = "MCQ",
            questionText = "Which 0.1 M aqueous solution will exhibit the highest boiling point elevation?",
            optionsListJson = "0.1 M Glucose|0.1 M NaCl|0.1 M BaCl2|0.1 M Al2(SO4)3",
            correctAnswer = "0.1 M Al2(SO4)3",
            hint = "Elevation in boiling point ΔTb = i * Kb * m. Compare van't Hoff factor (i) for complete dissociation.",
            solutionExplanation = "For Al2(SO4)3 -> 2Al³⁺ + 3SO4²⁻, i = 5. For BaCl2 i = 3, NaCl i = 2, Glucose i = 1. Highest 'i' gives highest colligative elevation in boiling point.",
            isBookmarked = false
        ),
        QuestionEntity(
            id = 6,
            subject = "Mathematics",
            chapter = "Matrices",
            topic = "Inverse & Determinants",
            difficulty = "Medium",
            questionType = "MCQ",
            questionText = "If A is a 3x3 non-singular square matrix and |A| = 4, then value of |adj(A)| is:",
            optionsListJson = "4|16|64|12",
            correctAnswer = "16",
            hint = "Formula for determinant of adjoint matrix of order n is |adj(A)| = |A|^(n-1).",
            solutionExplanation = "Here n = 3 and |A| = 4. Therefore |adj(A)| = |A|^(3 - 1) = |A|^2 = 4^2 = 16.",
            isBookmarked = false
        ),
        QuestionEntity(
            id = 7,
            subject = "Mathematics",
            chapter = "Relations and Functions",
            topic = "Equivalence Relations",
            difficulty = "Easy",
            questionType = "MCQ",
            questionText = "A relation R on set A = {1, 2, 3} given by R = {(1,1), (2,2), (3,3), (1,2), (2,1)} is:",
            optionsListJson = "Reflexive and Symmetric but not Transitive|Equivalence Relation|Reflexive only|Symmetric only",
            correctAnswer = "Equivalence Relation",
            hint = "Check reflexive (a,a), symmetric ((a,b)=>(b,a)), and transitive ((a,b)&(b,c)=>(a,c)).",
            solutionExplanation = "Reflexive since (1,1), (2,2), (3,3) in R. Symmetric since (1,2) and (2,1) both in R. Transitive: (1,2) & (2,1) => (1,1) in R, (2,1) & (1,2) => (2,2) in R. Hence R is an Equivalence Relation.",
            isBookmarked = false
        )
    )

    val initialTargets = listOf(
        StudyTargetEntity(
            id = 1,
            subject = "Physics",
            chapter = "Current Electricity",
            task = "Solve 10 Board PYQs on Kirchhoff's Laws & Potentiometer",
            priority = "High",
            estimatedMinutes = 45,
            deadlineDate = "Today",
            status = "Pending"
        ),
        StudyTargetEntity(
            id = 2,
            subject = "Chemistry",
            chapter = "Electrochemistry",
            task = "Revise Nernst Equation numericals and EMF calculation",
            priority = "High",
            estimatedMinutes = 40,
            deadlineDate = "Today",
            status = "Pending"
        ),
        StudyTargetEntity(
            id = 3,
            subject = "Mathematics",
            chapter = "Matrices",
            task = "Complete NCERT Exercise 3.4 & Practice test",
            priority = "Medium",
            estimatedMinutes = 50,
            deadlineDate = "Today",
            status = "Completed"
        ),
        // Overdue backlog item
        StudyTargetEntity(
            id = 4,
            subject = "Physics",
            chapter = "Electrostatic Potential and Capacitance",
            task = "Derivation of Energy stored in Capacitor and Dielectric effect",
            priority = "High",
            estimatedMinutes = 35,
            deadlineDate = "2 days ago",
            status = "Overdue",
            isBacklog = true
        )
    )

    val initialNotes = listOf(
        NoteEntity(
            id = 1,
            subject = "Physics",
            chapter = "Current Electricity",
            title = "Formula Cheat Sheet: Current Electricity",
            content = "1. Current: I = q/t = n * A * e * vd\n2. Drift Velocity: vd = (e * E * τ) / m\n3. Resistivity: ρ = m / (n * e^2 * τ)\n4. Resistance: R = ρ * (l / A)\n5. Temperature Coeff: R_t = R_0 * (1 + α * ΔT)\n6. Kirchhoff's Junction Rule: Σ I_in = Σ I_out\n7. Kirchhoff's Loop Rule: Σ ΔV = 0\n8. Wheatstone Bridge: P/Q = R/S under balanced galvanometer condition.",
            noteType = "Formula Sheet"
        )
    )
}
