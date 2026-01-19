package io.github.setheclark.intellij.mcp.prompts

import io.modelcontextprotocol.kotlin.sdk.types.*

/**
 * Prompt: generate_test_mocks
 *
 * Guides the LLM through generating test mocks from real network calls.
 * The LLM creates the mock fixtures - we just guide the process.
 */
fun createGenerateTestMocksPrompt() = Prompt(
    name = "generate_test_mocks",
    description = "Generate test mock data from real network calls for use in unit and integration tests.",
    arguments = listOf(
        PromptArgument(
            name = "feature",
            description = "Description of the feature to generate mocks for (e.g., 'login flow', 'user profile')",
            required = true
        ),
        PromptArgument(
            name = "startTime",
            description = "Optional: When the feature was used (epoch ms, for filtering relevant calls)",
            required = false
        ),
        PromptArgument(
            name = "endTime",
            description = "Optional: End of time range (epoch ms)",
            required = false
        )
    )
)

/**
 * Handler for the generate_test_mocks prompt.
 */
fun handleGenerateTestMocksPrompt(request: GetPromptRequest): GetPromptResult {
    val feature = request.arguments?.get("feature") ?: "Unknown Feature"
    val startTime = request.arguments?.get("startTime")
    val endTime = request.arguments?.get("endTime")

    val hasTimeRange = startTime != null && endTime != null

    val workflow = """
You are generating test mocks for: **$feature**

${if (hasTimeRange) "Time range: $startTime to $endTime" else "No specific time range provided - will search all calls"}

Follow this workflow to create useful test fixtures:

## Step 1: Find Relevant Network Calls
${if (hasTimeRange) {
    """
Use `get_network_calls` with:
- startTimeAfter: $startTime
- startTimeBefore: $endTime
- format: "summary"
"""
} else {
    """
Use `get_network_calls` with appropriate filters:
- Search for relevant URLs (e.g., for "login flow", look for `/auth/`, `/login`)
- Consider hasFailure: false (for success mocks) and hasFailure: true (for error mocks)
- Use format: "summary" to browse
"""
}}

Present the matching calls to the user and ask which ones they want to mock.

## Step 2: Get Full Details for Selected Calls
Once user selects call IDs, use `get_network_call_details`:
- callIds: [selected IDs]
- includeRequestBody: true
- includeResponseBody: true
- includeHeaders: true

## Step 3: Generate Mock Fixtures
For each call, create a well-structured mock fixture:

**Success Mock Format:**
```json
{
  "name": "descriptive_name_success",
  "description": "Mock for [what this tests]",
  "request": {
    "method": "GET",
    "url": "/api/endpoint",
    "headers": {
      "Authorization": "Bearer token",
      "Content-Type": "application/json"
    },
    "body": { ... } // if applicable
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "body": { ... }
  }
}
```

**Error Mock Variants:**
For each successful mock, consider generating error variants:
- 401 Unauthorized: Authentication failure
- 400 Bad Request: Validation error
- 500 Server Error: Backend failure
- 404 Not Found: Resource missing

## Step 4: Generate Test Code Structure
Based on the testing framework (ask user if unclear), provide example test code:

**For Jest/JavaScript:**
```javascript
describe('$feature', () => {
  it('should handle success case', async () => {
    // Mock the API call with the fixture
    fetchMock.mockResponseOnce(JSON.stringify(mockData.response.body), {
      status: mockData.response.status
    });

    // Test your code
    const result = await yourFunction();
    expect(result).toEqual(expectedResult);
  });
});
```

**For JUnit/Kotlin:**
```kotlin
@Test
fun `test $feature success`() {
    // Setup mock server with fixture
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(mockData.response.status)
            .setBody(mockData.response.body)
    )

    // Test your code
    val result = repository.doSomething()
    assertEquals(expectedResult, result)
}
```

## Step 5: Sanitize Sensitive Data
**IMPORTANT:** Check mocks for sensitive data and replace:
- Authentication tokens → "mock_token_xxx"
- API keys → "mock_api_key"
- Personal information (emails, names) → "test@example.com", "Test User"
- IDs → Consistent mock IDs that make sense in tests

## Step 6: Organize and Document
Provide:
1. **Mock Fixtures:** JSON files for each scenario (success + error variants)
2. **File Organization:** Suggest where to save mocks (e.g., `__mocks__/api/`)
3. **Usage Examples:** Code snippets showing how to use the mocks
4. **Test Scenarios:** List of test cases these mocks enable

## Output Format
Present as:
1. **Summary:** How many mocks generated, what scenarios covered
2. **Mock Fixtures:** Complete JSON for each mock
3. **Test Code Examples:** Framework-specific usage examples
4. **Next Steps:** How to integrate into existing test suite

Make mocks **realistic** (based on actual data) but **safe** (no sensitive info) and **useful** (cover important scenarios).
""".trimIndent()

    return GetPromptResult(
        description = "Generating test mocks for $feature",
        messages = listOf(
            PromptMessage(
                role = Role.User,
                content = TextContent(workflow)
            )
        )
    )
}
