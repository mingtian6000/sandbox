"""
Confluence Data Center REST API client.

Uses the v1 API at {base_url}/rest/api/...
Authentication: Basic Auth (username:password or username:api-token)
"""

import json
import os
import base64
from typing import Optional

import requests


class ConfluenceError(Exception):
    """Raised on Confluence API errors with HTTP status and detail."""

    def __init__(self, status_code: int, message: str, response_body: str = ""):
        self.status_code = status_code
        self.message = message
        self.response_body = response_body
        super().__init__(f"[{status_code}] {message}")


class ConfluenceClient:
    """HTTP client for Confluence Data Center REST API."""

    def __init__(
        self,
        base_url: Optional[str] = None,
        username: Optional[str] = None,
        token: Optional[str] = None,
    ):
        """Initialize from explicit params or environment variables.

        Env vars: CONFLUENCE_BASE_URL, CONFLUENCE_USERNAME, CONFLUENCE_TOKEN
        """
        self.base_url = (base_url or os.environ["CONFLUENCE_BASE_URL"]).rstrip("/")
        self.username = username or os.environ["CONFLUENCE_USERNAME"]
        self.token = token or os.environ["CONFLUENCE_TOKEN"]

        # Prepare Basic Auth header
        auth_raw = f"{self.username}:{self.token}"
        auth_b64 = base64.b64encode(auth_raw.encode()).decode()
        self._auth_header = {"Authorization": f"Basic {auth_b64}"}

        self._session = requests.Session()
        self._session.headers.update({
            **self._auth_header,
            "Accept": "application/json",
            "Content-Type": "application/json",
        })

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _url(self, path: str) -> str:
        return f"{self.base_url}/rest/api{path}"

    def _request(
        self,
        method: str,
        path: str,
        params: Optional[dict] = None,
        json_data: Optional[dict] = None,
        files: Optional[dict] = None,
        headers: Optional[dict] = None,
    ) -> dict:
        """Make an HTTP request and return parsed JSON response."""
        url = self._url(path)

        # For multipart uploads, let requests set Content-Type
        req_headers = dict(self._session.headers)
        if files:
            req_headers.pop("Content-Type", None)
        if headers:
            req_headers.update(headers)

        try:
            resp = self._session.request(
                method=method.upper(),
                url=url,
                params=params,
                json=json_data,
                files=files,
                headers=req_headers,
                timeout=60,
            )
        except requests.exceptions.ConnectionError as exc:
            raise ConfluenceError(0, f"Connection failed: {exc}")

        # Some endpoints return 204 No Content on success
        if resp.status_code == 204:
            return {"_success": True, "status_code": 204}

        # Try JSON first; fall back to text
        try:
            data = resp.json()
        except (json.JSONDecodeError, requests.exceptions.JSONDecodeError):
            data = {"_raw": resp.text}

        if not resp.ok:
            msg = data.get("message", data.get("_raw", "Unknown error"))
            raise ConfluenceError(resp.status_code, str(msg), resp.text)

        if isinstance(data, list):
            return {"_list": data, "status_code": resp.status_code}
        if not isinstance(data, dict):
            return {"_raw": str(data), "status_code": resp.status_code}

        data["status_code"] = resp.status_code
        return data

    # ------------------------------------------------------------------
    # Public API methods
    # ------------------------------------------------------------------

    def create_page(
        self,
        title: str,
        space_key: str,
        body_storage: str,
        parent_id: Optional[str] = None,
    ) -> dict:
        """Create a new page."""
        ancestors = []
        if parent_id:
            ancestors = [{"id": parent_id}]

        payload = {
            "type": "page",
            "title": title,
            "space": {"key": space_key},
            "ancestors": ancestors,
            "body": {
                "storage": {
                    "value": body_storage,
                    "representation": "storage",
                }
            },
        }
        return self._request("POST", "/content", json_data=payload)

    def update_page(
        self,
        page_id: str,
        body_storage: str,
        title: Optional[str] = None,
        version_bump: bool = True,
    ) -> dict:
        """Update an existing page's content and/or title.

        Confluence requires us to send the **current version number** plus 1.
        We fetch the current version first.
        """
        # Fetch current page to get version
        current = self.get_page(page_id, expand="version")
        current_version = current.get("version", {}).get("number", 0)
        new_version = current_version + 1 if version_bump else current_version

        payload = {
            "id": page_id,
            "type": "page",
            "title": title or current.get("title", ""),
            "version": {"number": new_version},
            "body": {
                "storage": {
                    "value": body_storage,
                    "representation": "storage",
                }
            },
        }
        return self._request("PUT", f"/content/{page_id}", json_data=payload)

    def delete_page(self, page_id: str) -> dict:
        """Delete a page by ID."""
        return self._request("DELETE", f"/content/{page_id}")

    def get_page(self, page_id: str, expand: Optional[str] = None) -> dict:
        """Get page details.

        Useful expand values:
          - "version,body.storage,space,ancestors,metadata.labels"
        """
        params = {"expand": expand} if expand else None
        return self._request("GET", f"/content/{page_id}", params=params)

    def get_page_children(
        self,
        parent_id: str,
        limit: int = 50,
        start: int = 0,
    ) -> dict:
        """Get child pages of a given parent."""
        params = {
            "limit": min(limit, 200),
            "start": start,
        }
        return self._request("GET", f"/content/{parent_id}/child/page", params=params)

    def search_pages(
        self,
        cql: str,
        limit: int = 50,
        start: int = 0,
        expand: Optional[str] = None,
    ) -> dict:
        """Search pages using Confluence Query Language (CQL).

        Examples:
          - text ~ "keyword"
          - space = "DEV"
          - parent = "123456"
          - text ~ "bug" AND space = "DEV"
        """
        params = {
            "cql": cql,
            "limit": min(limit, 200),
            "start": start,
        }
        if expand:
            params["expand"] = expand
        return self._request("GET", "/content/search", params=params)

    def move_page(
        self,
        page_id: str,
        position: str,
        target_id: str,
    ) -> dict:
        """Move a page relative to another page.

        Args:
            page_id: The page to move
            position: "before", "after", "append", or "top"
            target_id: Target page (sibling for before/after, parent for append/top)
        """
        valid = {"before", "after", "append", "top"}
        if position not in valid:
            raise ValueError(f"position must be one of {valid}")
        return self._request("PUT", f"/content/{page_id}/move/{position}/{target_id}")

    def upload_attachment(
        self,
        page_id: str,
        file_path: str,
        comment: Optional[str] = None,
    ) -> dict:
        """Upload a file as an attachment to a page.

        Confluence DC uses multipart/form-data for attachments.
        """
        if not os.path.isfile(file_path):
            raise FileNotFoundError(f"Attachment file not found: {file_path}")

        filename = os.path.basename(file_path)
        with open(file_path, "rb") as f:
            files = {
                "file": (filename, f, "application/octet-stream"),
            }
            params = {}
            if comment:
                params["comment"] = comment

            try:
                resp = self._session.request(
                    "POST",
                    self._url(f"/content/{page_id}/child/attachment"),
                    params=params,
                    files=files,
                    headers={
                        **self._auth_header,
                        "Accept": "application/json",
                        "X-Atlassian-Token": "no-check",
                    },
                    timeout=120,
                )
            except requests.exceptions.ConnectionError as exc:
                raise ConfluenceError(0, f"Attachment upload connection failed: {exc}")

        if resp.status_code in (200, 201):
            try:
                return resp.json()
            except json.JSONDecodeError:
                return {"_success": True, "status_code": resp.status_code}
        try:
            data = resp.json()
        except json.JSONDecodeError:
            data = {"_raw": resp.text}

        msg = data.get("message", data.get("_raw", "Attachment upload failed"))
        raise ConfluenceError(resp.status_code, str(msg), resp.text)

    def get_labels(self, page_id: str) -> dict:
        """Get all labels on a page."""
        return self._request("GET", f"/content/{page_id}/label")

    def add_labels(self, page_id: str, labels: list[str]) -> dict:
        """Add labels to a page.

        Labels in Confluence DC are simple strings (no prefix).
        """
        payload = [{"name": label} for label in labels]
        return self._request("POST", f"/content/{page_id}/label", json_data=payload)

    def remove_label(self, page_id: str, label: str) -> dict:
        """Remove a label from a page.

        The label name must be URL-encoded in the path.
        """
        from urllib.parse import quote
        return self._request("DELETE", f"/content/{page_id}/label/{quote(label)}")

    def copy_page(
        self,
        page_id: str,
        target_parent_id: str,
        new_title: Optional[str] = None,
        target_space_key: Optional[str] = None,
    ) -> dict:
        """Copy a single page (without children) to a new parent.

        Steps:
          1. Fetch source page content + title + space
          2. Create a new page at target parent with same content

        Args:
            page_id: Source page to copy
            target_parent_id: Destination parent page ID
            new_title: Optional new title (defaults to original title)
            target_space_key: Optional target space (defaults to source space)
        """
        # Fetch source
        source = self.get_page(page_id, expand="body.storage,space")
        source_title = source.get("title", "")
        storage_value = (
            source.get("body", {})
            .get("storage", {})
            .get("value", "")
        )
        source_space = source.get("space", {}).get("key", "")

        title = new_title or source_title
        space_key = target_space_key or source_space

        return self.create_page(
            title=title,
            space_key=space_key,
            body_storage=storage_value,
            parent_id=target_parent_id,
        )

    def get_all_children_ordered(self, parent_id: str) -> list[dict]:
        """Get all child pages of a parent, preserving their Confluence ordering.

        Returns list of child page dicts with at minimum "id" and "title".
        Handles pagination automatically.
        """
        children = []
        start = 0
        limit = 200

        while True:
            resp = self.get_page_children(parent_id, limit=limit, start=start)
            results = resp.get("results", [])
            children.extend(results)

            if len(results) < limit:
                break
            start += limit

        return children
