package com.example.data

data class SampleProfile(
    val title: String,
    val description: String,
    val cvText: String,
    val targetRole: String
)

object SampleCVs {
    val samples = listOf(
        SampleProfile(
            title = "The Buzzword Veteran",
            description = "Junior Dev with 50 adjectives and zero quantifiable metrics",
            targetRole = "Senior Full Stack Engineer",
            cvText = """
Alex Morgan
Passionate, results-driven, highly dynamic and motivated Software Developer with a track record of synergy.
Proven ability to collaborate in fast-paced environments as a team player. Excellent communication skills and hard-working attitude.

Experience:
Software Developer | Apex Tech (2022 - Present)
- Responsible for managing web application components using React and JavaScript.
- Worked with cross-functional teams to brainstorm innovative solutions.
- Handled various bugs and assisted senior developers with tasks.
- Participated in daily agile standup meetings and contributed positive energy.
- Utilized Git, Docker, and AWS for various deployment procedures.

Junior Web Intern | Nova Solutions (2021 - 2022)
- Responsible for assisting with website updates and CSS changes.
- Did research on cutting-edge technologies.
- Helped make sure the app was responsive.

Skills:
React, JavaScript, TypeScript, HTML, CSS, Git, Docker, AWS, Team Player, Hard Working, Multitasking, Problem Solving.
            """.trimIndent()
        ),
        SampleProfile(
            title = "The Vague Project Manager",
            description = "Led 'various initiatives' with mysterious unquantified outcomes",
            targetRole = "Lead Technical Product Manager",
            cvText = """
Jordan Lee - Project Manager
Experienced professional with a demonstrated history of delivering value and managing deliverables. Skilled at bridging the gap between business and technology.

Experience:
Project Manager | Global Horizons (2020 - 2024)
- Responsible for overseeing project timelines and deliverables for multiple stakeholder groups.
- Managed team members across departments to align with corporate goals.
- Facilitated meetings and drafted status report summaries for leadership.
- Drove digital transformation initiatives throughout the organization.
- Implemented modern workflow strategies to optimize throughput.

Project Coordinator | Bright Ideas Co (2018 - 2020)
- Assisted project directors with day-to-day administrative tasks.
- Monitored project milestones and logged status changes.

Skills:
Agile, Scrum, JIRA, Communication, Leadership, Project Management, Cross-functional Coordination.
            """.trimIndent()
        ),
        SampleProfile(
            title = "The Understated Data Scientist",
            description = "Ph.D. level work described in one-line bullet points",
            targetRole = "Machine Learning Research Engineer",
            cvText = """
Taylor Chen
Data Scientist & Researcher

Experience:
Data Scientist | DeepMatrix Labs (2021 - Present)
- Built ML models in Python.
- Cleaned tabular data and trained neural networks.
- Used PyTorch and SQL for queries.
- Looked at customer churn predictions.

Research Assistant | City University (2019 - 2021)
- Did statistical tests on clinical datasets.
- Wrote code to automate data pipelines.
- Published papers on computer vision.

Skills:
Python, PyTorch, TensorFlow, SQL, Pandas, NumPy, Statistics.
            """.trimIndent()
        )
    )
}
