package io.github.setheclark.intellij.mcp.prompts

import io.modelcontextprotocol.kotlin.sdk.types.*

/**
 * Prompt: analyze_api_health
 *
 * Guides the LLM through comprehensive health analysis of API usage.
 * Combines failure analysis, performance analysis, and general health metrics.
 */
fun createAnalyzeApiHealthPrompt() = Prompt(
    name = "analyze_api_health",
    description = "Perform comprehensive health analysis of API usage including failure rates, performance metrics, and optimization opportunities.",
    arguments = listOf(
        PromptArgument(
            name = "timeWindowHours",
            description = "How many hours of history to analyze (default: 24)",
            required = false
        )
    )
)

/**
 * Handler for the analyze_api_health prompt.
 */
fun handleAnalyzeApiHealthPrompt(request: GetPromptRequest): GetPromptResult {
    val timeWindowHours = request.arguments?.get("timeWindowHours")?.toIntOrNull() ?: 24
    val timeWindowMs = timeWindowHours * 60 * 60 * 1000L
    val startTime = System.currentTimeMillis() - timeWindowMs

    val workflow = """
You are performing a comprehensive API health analysis for the last $timeWindowHours hours.

Follow this workflow to generate an actionable health report:

## Step 1: Overall Metrics
Use `get_network_calls` with:
- startTimeAfter: $startTime
- format: "summary"

Calculate:
- **Total Calls:** Count of all requests
- **Success Rate:** Percentage of successful vs failed calls
- **Average Response Time:** Mean duration across all calls
- **Total Data Transfer:** Sum of request + response sizes

## Step 2: Failure Analysis
Use `get_network_calls` with:
- startTimeAfter: $startTime
- hasFailure: true
- format: "detailed"

For each failed call, classify the failure:
- **Authentication (401/403):** Token/permission issues
- **Validation (400):** Bad request data
- **Server Errors (500-504):** Backend issues
- **Timeouts:** Requests taking too long
- **Network Errors:** Connection problems

Group failures by:
- **By Type:** Count of each failure pattern
- **By Endpoint:** Which APIs have most failures
- **By Time:** Are failures clustered at specific times?

## Step 3: Performance Analysis
From the successful calls, identify:

**Slow Endpoints:**
- List endpoints by average duration (slowest first)
- Flag any requests taking > 2 seconds
- Identify p95 and p99 latencies

**Large Payloads:**
- List endpoints by response size (largest first)
- Flag responses > 100KB
- Identify compression opportunities

**Performance Outliers:**
- Find individual calls with unusually long duration
- Check if specific requests are problematic vs endpoint in general

## Step 4: Pattern Detection
Look for problematic patterns:

**Redundancy:**
- Multiple identical requests within short timeframes
- Potential caching opportunities

**Waterfalls:**
- Sequential calls that could run in parallel
- Dependencies that could be optimized

**Chatty APIs:**
- Many small requests that could be batched
- Multiple queries that could be combined

**Temporal Patterns:**
- Spikes in failures or slow requests at specific times
- Potential infrastructure issues

## Step 5: Health Scoring
Assign a health score (0-100) based on:
- Success Rate: 50% weight (>98% = excellent, 95-98% = good, 90-95% = fair, <90% = poor)
- Performance: 30% weight (<500ms avg = excellent, <1s = good, <2s = fair, >2s = poor)
- Failure Patterns: 20% weight (no patterns = excellent, minor issues = good, concerning patterns = poor)

## Step 6: Prioritized Recommendations
Generate actionable recommendations ranked by impact:

**Critical (Fix Immediately):**
- Endpoints with >10% failure rate
- Requests consistently timing out
- Authentication issues affecting users
- Server errors indicating backend problems

**High Priority:**
- Endpoints with >2s average response time
- Redundant calls wasting >5 seconds total
- Multiple failures clustered in time (infrastructure issue)

**Medium Priority:**
- Opportunities to cache frequently-called endpoints
- Batch similar requests
- Optimize large payloads

**Low Priority:**
- General performance improvements
- Code cleanup opportunities

## Output Format
Present as a structured health report:

### API Health Report ($timeWindowHours hours)

**Overall Health Score:** X/100 (Excellent/Good/Fair/Poor)

**Metrics Summary:**
- Total Calls: X
- Success Rate: X%
- Average Response Time: Xms
- Total Data Transfer: XMB

**Failure Analysis:**
- Total Failures: X (X%)
- By Type: [breakdown]
- Top Failing Endpoints: [list]
- Failure Clusters: [if any]

**Performance Analysis:**
- Slowest Endpoints: [top 5 with avg time]
- Largest Responses: [top 5 with sizes]
- Performance Outliers: [specific calls]

**Detected Issues:**
- 🔴 Critical: [list with impact]
- 🟠 High Priority: [list]
- 🟡 Medium Priority: [list]

**Recommendations:**
1. [Priority] [Specific action] - Expected impact: [description]
2. ...

**Trends:**
- [Any notable patterns or changes]

Focus on actionable insights that help developers improve API reliability and performance.
""".trimIndent()

    return GetPromptResult(
        description = "Comprehensive API health analysis for last $timeWindowHours hours",
        messages = listOf(
            PromptMessage(
                role = Role.User,
                content = TextContent(workflow)
            )
        )
    )
}
