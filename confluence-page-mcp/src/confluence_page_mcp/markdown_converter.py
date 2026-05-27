"""
Markdown → Confluence Storage Format converter.

Confluence Storage Format uses XHTML (a subset of HTML4 + Confluence-specific elements).

- Plain markdown → HTML → acceptable as storage format for most content
- Code blocks get special treatment: wrapped in Confluence <ac:structured-macro> macro
- Tables, lists, headings all map directly
"""

import re
import html as html_mod
from typing import Optional

import markdown as md_lib


MARKDOWN_EXTENSIONS = [
    "extra",           # tables, footnotes, attr_list, etc.
    "codehilite",      # code blocks with language hint
    "sane_lists",      # smarter list behavior
    "toc",             # table of contents
]


def markdown_to_storage(md_text: str) -> str:
    """Convert Markdown text to Confluence Storage Format XHTML.

    Steps:
    1. Convert markdown → HTML via python-markdown
    2. Post-process <pre><code> → Confluence <ac:structured-macro ac:name="code">
    3. Return as a CDATA-safe string the Confluence API accepts.
    """
    # Convert markdown to HTML
    html_content = md_lib.markdown(md_text, extensions=MARKDOWN_EXTENSIONS)

    # Post-process code blocks
    html_content = _convert_code_blocks(html_content)

    return html_content


def html_to_storage(html_text: str) -> str:
    """Wrap raw HTML as Confluence Storage Format.

    Basic validation: ensure content is wrapped in a block-level container.
    """
    # Strip any <html>/<body> wrappers if present
    html_text = re.sub(r"</?(?:html|body)[^>]*>", "", html_text, flags=re.IGNORECASE | re.DOTALL)

    # If it's just a single text node (no HTML tags), wrap in <p>
    stripped = html_text.strip()
    if not re.search(r"<[a-zA-Z/!?]", stripped):
        return f"<p>{html_mod.escape(stripped)}</p>"

    return stripped


def is_valid_storage(value: str) -> bool:
    """Rough check: does the string look like valid Confluence Storage XML?"""
    if not value or not value.strip():
        return False
    # Must contain at least one block-level element
    block_tags = (
        r"<(?:p|h[1-6]|ul|ol|li|table|tr|td|th|div|pre|"
        r"ac:structured-macro|ac:plain-text-body|ac:parameter|ac:link|ac:image)\b"
    )
    return bool(re.search(block_tags, value, re.IGNORECASE))


def _convert_code_blocks(html: str) -> str:
    """Convert <pre><code class="language-xxx"> to Confluence code macro."""
    def _replace_code(match: re.Match) -> str:
        pre_content = match.group(1)

        # Extract language from <code class="language-xxx">
        lang_match = re.search(
            r'<code[^>]*class\s*=\s*["\'](?:[^"\']*\s)?language-(\w+)',
            pre_content,
            re.IGNORECASE,
        )
        language = lang_match.group(1) if lang_match else ""

        # Extract code text
        code_content = re.sub(
            r"</?code[^>]*>", "", pre_content
        ).strip()

        escaped = html_mod.unescape(code_content)
        escaped_xml = (
            escaped
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        )

        if language:
            return (
                f'<ac:structured-macro ac:name="code">\n'
                f'  <ac:parameter ac:name="language">{html_mod.escape(language)}</ac:parameter>\n'
                f'  <ac:plain-text-body>\n'
                f'    <![CDATA[{escaped}]]>\n'
                f'  </ac:plain-text-body>\n'
                f'</ac:structured-macro>'
            )
        else:
            return (
                f'<ac:structured-macro ac:name="code">\n'
                f'  <ac:plain-text-body>\n'
                f'    <![CDATA[{escaped}]]>\n'
                f'  </ac:plain-text-body>\n'
                f'</ac:structured-macro>'
            )

    # Match <pre>...</pre> blocks
    result = re.sub(
        r"<pre>(.*?)</pre>",
        _replace_code,
        html,
        flags=re.IGNORECASE | re.DOTALL,
    )
    return result


def to_storage_content(content: str, content_format: str = "markdown") -> str:
    """Unified converter: accept markdown, html, or storage, always return storage XHTML.

    Args:
        content: The content string
        content_format: One of "markdown", "html", "storage"

    Returns:
        Storage-format XHTML string
    """
    fmt = content_format.lower().strip()

    if fmt == "storage":
        if not is_valid_storage(content):
            raise ValueError(
                "Content does not look like valid Confluence Storage Format. "
                "Use format='markdown' or format='html' for simpler input."
            )
        return content
    elif fmt == "html":
        return html_to_storage(content)
    elif fmt == "markdown":
        return markdown_to_storage(content)
    else:
        raise ValueError(
            f"Unknown content_format: '{content_format}'. "
            f"Use 'markdown', 'html', or 'storage'."
        )
