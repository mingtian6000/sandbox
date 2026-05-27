# confluence-page-mcp

FastMCP server for Confluence Data Center page management — create, update, copy,
delete, search, rearrange, attach files, and label pages via the Confluence REST API.

## Features

| Tool | Description |
|------|-------------|
| `create_page` | Create a page with markdown/HTML/storage content |
| `update_page` | Update existing page content (and optionally title) |
| `delete_page` | Delete a page by ID |
| `get_page` | Get page details (with optional expand) |
| `copy_page` | Copy a page to a new parent (single page, no children) |
| `search_pages` | CQL-based page search, optionally filtered by parent/space |
| `get_page_children` | List child pages of a given parent |
| `rearrange_children` | Reorder sibling pages under a parent |
| `upload_attachment` | Upload a file attachment to a page |
| `add_labels` | Add labels/tags to a page |

## Setup

### 1. Clone & Install

```bash
cd confluence-page-mcp
pip install -r requirements.txt
```

### 2. Configure Environment

Set these environment variables (or put them in your MCP client config):

```bash
export CONFLUENCE_BASE_URL="https://confluence.yourcompany.com"
export CONFLUENCE_USERNAME="your-email@example.com"
export CONFLUENCE_TOKEN="your-api-token-or-password"
```

### 3. Add to MCP Client

**Claude Desktop / Cline / etc.** — add to your MCP server config:

```json
{
  "mcpServers": {
    "confluence-page-mcp": {
      "command": "python",
      "args": ["/path/to/confluence-page-mcp/src/confluence_page_mcp/server.py"],
      "env": {
        "CONFLUENCE_BASE_URL": "https://confluence.yourcompany.com",
        "CONFLUENCE_USERNAME": "your-email@example.com",
        "CONFLUENCE_TOKEN": "your-api-token-or-password"
      }
    }
  }
}
```

### 4. Quick Test

```python
# via the MCP inspector or any MCP client
# Call: create_page(title="Hello", space_key="DEV", content="# Hello World")
```

## Content Format

All `create_page` / `update_page` calls accept three formats via the `content_format` parameter:

| Format | Description |
|--------|-------------|
| `markdown` | Write in Markdown — auto-converts to Confluence Storage Format |
| `html` | Provide raw HTML — needs to be Confluence-compatible XHTML |
| `storage` | Pass Confluence Storage Format XML directly (advanced) |

## API

### Confluence Data Center (Server)

This server uses the Confluence **Data Center (Server)** REST API v1 at `{base_url}/rest/api/...`.
It does **not** support Atlassian Cloud API v2.

## License

MIT
