package io.github.setheclark.intellij.mcp.prompts

import io.modelcontextprotocol.kotlin.sdk.types.*

/**
 * Prompt: debug_api_failure
 *
 * Guides the LLM through debugging a specific failed network request.
 * The LLM performs root cause analysis - we just provide the debugging workflow.
 */
fun createDebugApiFailurePrompt() = Prompt(
    name = "debug_api_failure",
    description = "Debug a specific failed API call to identify root cause and suggest fixes.",
    arguments = listOf(
        PromptArgument(
            name = "callId",
            description = "The call ID of the failed request to debug (required)",
            required = true
        )
    )
)

/**
 * Handler for the debug_api_failure prompt.
 */
fun handleDebugApiFailurePrompt(request: GetPromptRequest): GetPromptResult {
    val callId = request.arguments?.get("callId") ?: "MISSING"

    val workflow = """
You are debugging a failed API call. Follow this systematic workflow:

## Step 1: Get Failure Details
Use `get_network_call_details` with:
- callIds: ["$callId"]
- Include all details (headers, body)

Examine:
- Error message in the failure issue
- Request details (URL, method, headers, body)
- Duration (was it a timeout?)

## Step 2: Classify the Failure
Based on the error message, classify the failure type:
- **Authentication (401/403):** Token expired, invalid, or missing
- **Validation (400):** Invalid request data or missing required fields
- **Server Error (500-504):** Backend service issue
- **Timeout:** Request took too long
- **Network Error:** Connection problems

## Step 3: Find Similar Successful Calls
Use `get_network_calls` to find similar requests that succeeded:
- Same URL and method
- hasFailure: false
- Recent time window

If you find successful calls, proceed to Step 4. If none exist, this endpoint may always fail or be new.

## Step 4: Compare with Successful Call
If you found a successful call:
- Use `compare_network_calls` to diff the failed call with a successful one
- Identify key differences:
  - Headers (especially Authorization, Content-Type)
  - Request body format or content
  - Request parameters

## Step 5: Identify Root Cause
Based on failure type and comparison:

**Authentication Failures:**
- Missing or malformed Authorization header
- Expired token (check timestamps)
- Wrong authentication scheme (Bearer vs Basic)

**Validation Failures:**
- Missing required fields in request body
- Wrong data types or formats
- Invalid parameter values
- Incorrect Content-Type header

**Server Errors:**
- Backend service down or overloaded
- Database connection issues
- Unhandled exception in backend
- Check if this is isolated or part of a pattern (multiple failures at same time)

**Timeouts:**
- Request too complex or large
- Backend performance issue
- Network latency
- Timeout threshold too low

**Network Errors:**
- No internet connectivity
- DNS resolution failure
- Firewall blocking request
- Invalid URL or unreachable host

## Step 6: Provide Actionable Fixes
Give specific, actionable recommendations:
1. **Immediate fix:** What the developer should change now
2. **Verification:** How to test the fix
3. **Prevention:** How to avoid this in the future
4. **Investigation:** What to check if the fix doesn't work

## Output Format
Present as:
1. **Failure Summary:** Type, endpoint, error message
2. **Root Cause Analysis:** Your determination of what went wrong
3. **Evidence:** Key differences found (if comparison was possible)
4. **Recommended Fixes:** Numbered list of specific actions
5. **Additional Context:** Related failures, timing patterns, etc.

Be specific and actionable. Don't just say "check authentication" - explain exactly what to verify and how.
""".trimIndent()

    return GetPromptResult(
        description = "Debugging workflow for failed API call $callId",
        messages = listOf(
            PromptMessage(
                role = Role.User,
                content = TextContent(workflow)
            )
        )
    )
}
