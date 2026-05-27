"""
Confluence Page MCP — FastMCP Server

Exposes Confluence Data Center page management operations as MCP tools.

Environment Variables (set in MCP client config):
  CONFLUENCE_BASE_URL   e.g. https://confluence.yourcompany.com
  CONFLUENCE_USERNAME   e.g. admin@example.com
  CONFLUENCE_TOKEN      API token or password
"""

import json
import os
import sys
from typing import Optional

from mcp.server.fastmcp import FastMCP

from .confluence_client import ConfluenceClient, ConfluenceError
from .markdown_converter import to_storage_content


def _make_response(success: bool, data: any = None, error: str = None) -> str:
    """Build a consistent JSON response string."""
    result = {"success": success}
    if data is not None:
        result["data"] = data
    if error:
        result["error"] = error
    return json.dumps(result, indent=2, ensure_ascii=False)


def _get_client() -> ConfluenceClient:
    """Get or create the Confluence client (lazy singleton via app context)."""
    # FastMCP stores state in app.state
    app_attr = "_confluence_client"
    if not hasattr(mcp, app_attr):
        try:
            client = ConfluenceClient()
        except KeyError as e:
            missing = str(e).strip("'")
            raise RuntimeError(
                f"Missing required environment variable: {missing}. "
                f"Set CONFLUENCE_BASE_URL, CONFLUENCE_USERNAME, and "
                f"CONFLUENCE_TOKEN in your MCP client config."
            )
        setattr(mcp, app_attr, client)
    return getattr(mcp, app_attr)


# ---------------------------------------------------------------------------
# FastMCP Server
# ---------------------------------------------------------------------------

mcp = FastMCP(
    name="confluence-page-mcp",
    instructions=(
        "Confluence Page Management MCP Server for Data Center.\n\n"
        "Tools for creating, updating, deleting, copying, searching, rearranging, "
        "and managing attachments and labels on Confluence pages.\n\n"
        "Requires CONFLUENCE_BASE_URL, CONFLUENCE_USERNAME, CONFLUENCE_TOKEN "
        "environment variables."
    ),
    log_level="INFO",
)


# ---------------------------------------------------------------------------
# Tools
# ---------------------------------------------------------------------------


@mcp.tool(description="Create a new Confluence page. Supports markdown, HTML, or storage format content.")
def create_page(
    title: str,
    space_key: str,
    content: str,
    parent_id: Optional[str] = None,
    content_format: str = "markdown",
) -> str:
    """Create a new page in Confluence.

    Args:
        title: Page title
        space_key: Confluence space key (e.g. "DEV", "DOCS")
        content: Page content in the specified format
        parent_id: Optional parent page ID (omit for root-level page in space)
        content_format: One of "markdown", "html", "storage"

    Returns:
        JSON with created page details including id, title, and _links.
    """
    try:
        client = _get_client()
        storage = to_storage_content(content, content_format)
        result = client.create_page(title, space_key, storage, parent_id)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Update an existing Confluence page's content and/or title.")
def update_page(
    page_id: str,
    content: str,
    title: Optional[str] = None,
    content_format: str = "markdown",
) -> str:
    """Update a page's content (and optionally its title).

    Args:
        page_id: The numeric ID of the page to update
        content: New page content
        title: Optional new title (leave blank to keep existing)
        content_format: One of "markdown", "html", "storage"

    Returns:
        JSON with updated page details.
    """
    try:
        client = _get_client()
        storage = to_storage_content(content, content_format)
        result = client.update_page(page_id, storage, title)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Delete a Confluence page by ID. Irreversible.")
def delete_page(page_id: str) -> str:
    """Permanently delete a Confluence page.

    Args:
        page_id: The numeric ID of the page to delete

    Returns:
        JSON confirming deletion.
    """
    try:
        client = _get_client()
        client.delete_page(page_id)
        return _make_response(True, data={"deleted_page_id": page_id})
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Get a Confluence page's details including content, version, space, and metadata.")
def get_page(
    page_id: str,
    expand: Optional[str] = None,
) -> str:
    """Fetch page details from Confluence.

    Args:
        page_id: The numeric ID of the page
        expand: Optional comma-separated expand fields
                e.g. "version,body.storage,space,ancestors,metadata.labels"

    Returns:
        JSON with page data including title, version, body, space, and ancestors.
    """
    try:
        client = _get_client()
        result = client.get_page(page_id, expand=expand)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Copy a single Confluence page (without children) to a new parent.")
def copy_page(
    page_id: str,
    target_parent_id: str,
    new_title: Optional[str] = None,
    target_space_key: Optional[str] = None,
) -> str:
    """Copy a page to a new location. Only the single page is copied (no children).

    Args:
        page_id: Source page ID to copy
        target_parent_id: Destination parent page ID
        new_title: Optional new title (defaults to original)
        target_space_key: Optional target space (defaults to source space)

    Returns:
        JSON with the newly created page details.
    """
    try:
        client = _get_client()
        result = client.copy_page(page_id, target_parent_id, new_title, target_space_key)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Search Confluence pages using CQL (Confluence Query Language). Supports filtering by parent and space.")
def search_pages(
    query: str,
    parent_id: Optional[str] = None,
    space_key: Optional[str] = None,
    limit: int = 50,
    expand: Optional[str] = None,
) -> str:
    """Search pages with CQL.

    Builds a CQL query from the provided parameters.
    Simple text search: text ~ "keyword"
    Filter by parent:   parent = "123"
    Filter by space:    space = "KEY"

    Args:
        query: Free-text search query
        parent_id: Optional parent page ID to scope the search
        space_key: Optional space key to scope the search
        limit: Max results (up to 200)
        expand: Optional expand fields

    Returns:
        JSON with search results array.
    """
    try:
        client = _get_client()

        # Build CQL
        conditions = [f'text ~ "{_escape_cql(query)}"']
        if parent_id:
            conditions.append(f'parent = "{parent_id}"')
        if space_key:
            conditions.append(f'space = "{space_key}"')

        cql = " AND ".join(conditions)
        result = client.search_pages(cql, limit=limit, expand=expand)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="List all child pages of a given parent page, in their current order.")
def get_page_children(
    parent_id: str,
    limit: int = 50,
    expand: Optional[str] = None,
) -> str:
    """Get child pages of a parent, sorted by Confluence's current order.

    Args:
        parent_id: Parent page ID
        limit: Max results (up to 200)
        expand: Optional expand fields

    Returns:
        JSON with children results array.
    """
    try:
        client = _get_client()
        result = client.get_page_children(parent_id, limit=limit)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Reorder sibling pages under the same parent. Provide the desired order as a list of page IDs.")
def rearrange_children(
    parent_id: str,
    ordered_page_ids: list[str],
) -> str:
    """Reorder child pages under a parent.

    The first page ID in the list becomes the first child, the last becomes the last.
    Uses Confluence's move API (before/after positioning).

    Args:
        parent_id: Parent page whose children will be reordered
        ordered_page_ids: List of child page IDs in the desired order

    Returns:
        JSON with a summary of the reorder operations performed.
    """
    try:
        client = _get_client()

        if len(ordered_page_ids) < 2:
            return _make_response(True, data={
                "message": "Less than 2 pages to reorder. Nothing to do.",
                "parent_id": parent_id,
                "ordered_count": len(ordered_page_ids),
            })

        operations = []
        errors = []

        # Strategy: process from left to right.
        # For each page at index i (i>0), move it "after" the page at index i-1.
        # This builds the desired order incrementally.
        for i in range(1, len(ordered_page_ids)):
            child_id = ordered_page_ids[i]
            prev_id = ordered_page_ids[i - 1]
            try:
                client.move_page(child_id, position="after", target_id=prev_id)
                operations.append({"page_id": child_id, "position": f"after {prev_id}"})
            except ConfluenceError as e:
                errors.append({"page_id": child_id, "error": str(e)})
            except Exception as e:
                errors.append({"page_id": child_id, "error": str(e)})

        summary = {
            "parent_id": parent_id,
            "desired_order": ordered_page_ids,
            "operations_performed": len(operations),
            "errors": errors if errors else None,
        }

        if errors:
            summary["message"] = f"Reordered with {len(errors)} error(s)."
            return _make_response(True, data=summary)
        else:
            summary["message"] = "All pages reordered successfully."
            return _make_response(True, data=summary)

    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Upload a file as an attachment to a Confluence page.")
def upload_attachment(
    page_id: str,
    file_path: str,
    comment: Optional[str] = None,
) -> str:
    """Upload an attachment to a page.

    Args:
        page_id: Page ID to attach the file to
        file_path: Absolute or relative path to the file on disk
        comment: Optional attachment comment/description

    Returns:
        JSON with attachment details.
    """
    try:
        client = _get_client()
        result = client.upload_attachment(page_id, file_path, comment)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Add one or more labels (tags) to a Confluence page.")
def add_labels(
    page_id: str,
    labels: list[str],
) -> str:
    """Add labels to a page.

    Args:
        page_id: Page ID
        labels: List of label strings to add (e.g. ["howto", "documentation", "v2"])

    Returns:
        JSON with applied labels.
    """
    try:
        client = _get_client()
        result = client.add_labels(page_id, labels)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


@mcp.tool(description="Get labels attached to a Confluence page.")
def get_labels(page_id: str) -> str:
    """Fetch all labels on a page.

    Args:
        page_id: Page ID

    Returns:
        JSON with labels array.
    """
    try:
        client = _get_client()
        result = client.get_labels(page_id)
        return _make_response(True, data=result)
    except Exception as e:
        return _make_response(False, error=str(e))


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------

def main():
    """Run the MCP server.

    Uses stdio transport by default (for MCP client integration).
    Pass --sse to run as HTTP SSE server on a port.
    """
    use_sse = "--sse" in sys.argv
    port = 8080
    for i, arg in enumerate(sys.argv):
        if arg == "--port" and i + 1 < len(sys.argv):
            port = int(sys.argv[i + 1])

    transport = "sse" if use_sse else "stdio"
    mcp.run(transport=transport)


def _escape_cql(text: str) -> str:
    """Escape special characters inside a CQL quoted string."""
    return text.replace('\\', '\\\\').replace('"', '\\"')


if __name__ == "__main__":
    main()
