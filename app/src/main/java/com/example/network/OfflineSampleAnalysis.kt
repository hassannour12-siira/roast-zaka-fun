package com.example.network

import com.example.model.*

/**
 * A pre-written sample analysis, kept for demoing without a network or an API key.
 *
 * This is NOT analysis of the user's CV. It pattern-matches a few buzzwords and otherwise
 * returns fixed copy. It used to be served silently whenever a real API call failed, which
 * meant a bad key looked identical to a successful roast. It is now opt-in, and everything
 * it produces is flagged with isOfflineFallback = true so the UI can label it.
 */
object OfflineSampleAnalysis {

    fun generateTailoredAnalysis(
        cvText: String,
        targetRole: String,
        intensity: RoastIntensity
    ): FullAnalysisResult {
        val lowerText = cvText.lowercase()

        // Extract candidate name / headline
        val firstLine = cvText.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Candidate"
        val candidateName = if (firstLine.length < 35 && !firstLine.contains("resume", ignoreCase = true)) firstLine else "Candidate"

        val headline = when {
            lowerText.contains("react") || lowerText.contains("frontend") -> "Frontend Software Developer"
            lowerText.contains("product manager") || lowerText.contains("scrum") -> "Product & Delivery Specialist"
            lowerText.contains("data") || lowerText.contains("machine learning") -> "Data & Analytics Practitioner"
            targetRole.isNotBlank() -> "Aspiring $targetRole"
            else -> "Technology Professional"
        }

        // Buzzword detector
        val commonBuzzwords = listOf(
            "hardworking", "motivated", "team player", "responsible for",
            "results-driven", "passionate", "dynamic", "excellent communication skills",
            "synergy", "multitasking", "go-getter", "detail-oriented"
        )
        val detectedBuzzwords = commonBuzzwords.filter { lowerText.contains(it) }.ifEmpty {
            listOf("responsible for", "dynamic", "team player")
        }

        // Detect weak bullets from text
        val candidateBullets = cvText.lines()
            .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
            .filter { it.length in 25..140 && !it.startsWith("http") }

        val weakBullet1 = candidateBullets.firstOrNull { it.lowercase().contains("responsible for") || it.lowercase().contains("worked with") }
            ?: candidateBullets.firstOrNull()
            ?: "Responsible for assisting with daily operational tasks and managing team schedules."

        val weakBullet2 = candidateBullets.firstOrNull { it != weakBullet1 }
            ?: "Worked with cross-functional partners to improve processes."

        // Observations tailored to intensity
        val observations = when (intensity) {
            RoastIntensity.LIGHT -> listOf(
                RoastObservation(
                    title = "The 'Responsible For' Trap",
                    roast = "You used 'responsible for' like a safety blanket. We know what your job description was—tell us what you actually won.",
                    problem = "Stating responsibilities instead of achievements makes you sound like a passive bystander in your own career.",
                    severity = Severity.MEDIUM
                ),
                RoastObservation(
                    title = "Adjective Congestion",
                    roast = "Passionate, dynamic AND results-driven? Save some enthusiasm for the interview room!",
                    problem = "Generic praise adjectives crowd out the hard skills that recruiters' eyes look for in the first 6 seconds.",
                    severity = Severity.LOW
                ),
                RoastObservation(
                    title = "The Vanishing Metrics Mystery",
                    roast = "There are fewer numbers in your experience section than in a preschool picture book.",
                    problem = "Without scale, volume, or % improvements, recruiters cannot gauge the scope of your impact.",
                    severity = Severity.HIGH
                ),
                RoastObservation(
                    title = "Tech Buffet Syndrome",
                    roast = "You listed Docker, AWS, and Git right beside 'Team Player'. That's like listing a Ferrari next to good manners.",
                    problem = "Mixing technical tools with basic interpersonal traits dilutes the perceived seniority of your toolset.",
                    severity = Severity.MEDIUM
                )
            )
            RoastIntensity.SPICY -> listOf(
                RoastObservation(
                    title = "Corporate Buzzword Salad",
                    roast = "\"Results-driven, dynamic team player\"? Somewhere, an Applicant Tracking System just sighed deeply.",
                    problem = "Your profile leans heavily on LinkedIn clichés rather than verified deliverables.",
                    severity = Severity.HIGH
                ),
                RoastObservation(
                    title = "The 'Responsible For' Alibi",
                    roast = "'Responsible for managing components' tells me approximately nothing—and somehow wastes 8 words doing it.",
                    problem = "Phrasing tasks as passive duties hides your individual contribution and ownership.",
                    severity = Severity.HIGH
                ),
                RoastObservation(
                    title = "Zero Numbers in Sight",
                    roast = "Did your code serve 3 users or 3 million? Did you save \$10 or \$100K? Right now it reads like top-secret classified trivia.",
                    problem = "Lack of quantified evidence leaves recruiters guessing your operational scale.",
                    severity = Severity.HIGH
                ),
                RoastObservation(
                    title = "Skills List as a Wishlist",
                    roast = "You put AWS and Docker in your skills, but your work bullets sound like you just read the documentation on Wikipedia.",
                    problem = "Skills listed without matching project bullet evidence get discounted by technical interviewers.",
                    severity = Severity.MEDIUM
                ),
                RoastObservation(
                    title = "Vague Impact Statements",
                    roast = "\"Assisted senior developers with tasks\"—so you fetched the cold brew or you refactored the auth pipeline?",
                    problem = "Underselling or obscuring your specific responsibility creates hesitation in hiring managers.",
                    severity = Severity.MEDIUM
                )
            )
            RoastIntensity.EXTRA_SPICY -> listOf(
                RoastObservation(
                    title = "Emotional Support Buzzwords",
                    roast = "You used every buzzword in the dictionary except 'competent'. If LinkedIn had a tax on cliches, you'd be in debt.",
                    problem = "Zero substantive differentiators. Your opening reads like a generic template from 2014.",
                    severity = Severity.HIGH
                ),
                RoastObservation(
                    title = "Witness Protection Resume",
                    roast = "Are you in the witness protection program? Because your bullet points go out of their way to ensure no one knows what you actually accomplished.",
                    problem = "Passive, vague phrasing that describes company activities rather than personal ownership.",
                    severity = Severity.HIGH
                ),
                RoastObservation(
                    title = "Severe Metrics Deficiency",
                    roast = "I've seen grocery receipts with more quantitative analysis than this work history.",
                    problem = "Absence of metrics (revenue, latency, user counts, efficiency gains) forces recruiters to discard the CV.",
                    severity = Severity.HIGH
                ),
                RoastObservation(
                    title = "The 'Hard Working' Red Flag",
                    roast = "Listing 'hard-working' as a skill is like an airline advertising that their planes have wings. It's the baseline, not a brag.",
                    problem = "Listing table-stakes soft skills wastes prime page real estate.",
                    severity = Severity.MEDIUM
                ),
                RoastObservation(
                    title = "Diluted Career Narrative",
                    roast = "Your career narrative has more plot holes than a daytime soap opera. What are you actually an expert in?",
                    problem = "Unclear positioning between junior generalist and senior specialist.",
                    severity = Severity.HIGH
                )
            )
        }

        val openingLine = when (intensity) {
            RoastIntensity.LIGHT -> "A promising foundation that's hiding its best work behind modest, generic wording."
            RoastIntensity.SPICY -> "Your CV thinks it's ready. The recruiter's recycling bin had other opinions. 🔥"
            RoastIntensity.EXTRA_SPICY -> "Brace yourself. We've seen microwave manuals with more compelling career narratives. 💀"
        }

        val recruiterFirstImpression = "3 seconds in: 'Okay, seen this template 40 times today... Wait, did they actually accomplish anything or just hang out near the codebase?'"

        val scores = Scores(
            clarity = 66,
            specificity = 48,
            impact = 44,
            readability = 78,
            skillsEvidence = 56,
            jobAlignment = if (targetRole.isNotBlank()) 62 else null
        )

        val bulletRewrites = listOf(
            BulletRewrite(
                original = weakBullet1,
                improved = "Architected and delivered core UI features and interactive workflows, accelerating page render speeds and streamlining user navigation.",
                recommendation = "Add evidence: Insert user count, response time reduction (e.g. 25%), or sprint delivery velocity."
            ),
            BulletRewrite(
                original = weakBullet2,
                improved = "Partnered with product managers and engineering peers to resolve high-priority defects and implement maintainable system enhancements.",
                recommendation = "Add evidence: Mention bug volume reduction, customer satisfaction bump, or cycle time saved."
            )
        )

        val summary = SummaryRescue(
            original = "Passionate and dynamic professional with experience in modern technologies and strong teamwork.",
            improved = "Engineer with proven expertise in building resilient frontend systems and scalable web applications. Adept at turning product roadmaps into high-performance code with a focus on clean architecture and measurable reliability."
        )

        val demonstratedSkills = listOf("React", "JavaScript", "Git", "CSS", "UI Component Design").filter { lowerText.contains(it.lowercase()) }
            .ifEmpty { listOf("Frontend Development", "Component Architecture", "Version Control") }

        val unDemonstratedSkills = listOf("Docker", "AWS", "CI/CD", "TypeScript", "Microservices")
            .filter { lowerText.contains(it.lowercase()) }
            .ifEmpty { listOf("System Architecture", "Cloud Infrastructure") }

        val jobMatch = if (targetRole.isNotBlank()) {
            JobMatchData(
                enabled = true,
                strongMatches = demonstratedSkills.take(3),
                notDemonstrated = listOf("End-to-end cloud deployment pipelines", "High-throughput performance profiling"),
                keywordsPresent = listOf("React", "Git", "Agile", "APIs"),
                keywordsMissing = listOf("CI/CD Automation", "Test-Driven Development (TDD)", "Distributed Systems")
            )
        } else {
            JobMatchData(enabled = false)
        }

        return FullAnalysisResult(
            isOfflineFallback = true,
            candidate = CandidateInfo(
                name = candidateName,
                headline = headline,
                yearsExperience = "2-4 years demonstrated"
            ),
            roast = RoastData(
                openingLine = openingLine,
                observations = observations,
                recruiterFirstImpression = recruiterFirstImpression,
                buzzwords = detectedBuzzwords,
                biggestRedFlag = "Zero measurable achievements and heavy reliance on passive 'responsible for' descriptions."
            ),
            scores = scores,
            rescue = RescueData(
                topFixes = listOf(
                    "Replace every passive 'responsible for' with a punchy past-tense action verb (Engineered, Launched, Automated).",
                    "Add measurable scale: include users served, % latency improvements, or bug turnaround time.",
                    "Remove table-stakes adjectives (hardworking, team player) and replace them with technical evidence."
                ),
                summary = summary,
                bulletRewrites = bulletRewrites
            ),
            skills = SkillsAnalysis(
                demonstrated = demonstratedSkills,
                mentionedButNotDemonstrated = unDemonstratedSkills,
                recommendedEmphasis = listOf("Component Optimization", "State Management Patterns", "Performance Metrics")
            ),
            jobMatch = jobMatch,
            intensity = intensity,
            jobTarget = targetRole,
            rawCvSnippet = cvText.take(150)
        )
    }
}
