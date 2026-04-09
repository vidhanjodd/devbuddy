

const ACTION_META = {
    explain:  { icon: '💡', label: 'Explaining',    loading: 'Grok is reading your code...'     },
    optimize: { icon: '🚀', label: 'Optimizing',    loading: 'Grok is finding improvements...'  },
    bugs:     { icon: '🐛', label: 'Scanning Bugs', loading: 'Grok is hunting for bugs...'      },
};

function openModal() {
    document.getElementById('aiBackdrop').classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeModal() {
    document.getElementById('aiBackdrop').classList.remove('open');
    document.body.style.overflow = '';
}

document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeModal();
});

async function triggerAi(btn, action) {
    const card      = btn.closest('.ai-actions');
    const snippetId = card.dataset.id;
    const title     = card.dataset.title;
    const meta      = ACTION_META[action];

    // Reset & open modal
    setModalState('loading');
    document.getElementById('modalIcon').textContent        = meta.icon;
    document.getElementById('modalAction').textContent      = meta.label;
    document.getElementById('modalSnippetName').textContent = title;
    document.getElementById('loadingText').textContent      = meta.loading;
    openModal();

    try {
        const csrf   = document.getElementById('csrf');
        const token  = csrf.dataset.token;
        const header = csrf.dataset.header;

        const res = await fetch(`/ai/${action}`, {
            method:  'POST',
            headers: {
                'Content-Type': 'application/json',
                [header]: token
            },
            body: JSON.stringify({ snippetId: parseInt(snippetId) })
        });

        const data = await res.json();

        if (data.success) {
            document.getElementById('aiResult').innerHTML = formatAiResult(data.result);
            setModalState('result');
        } else {
            document.getElementById('errorText').textContent = data.error || 'Grok returned an unexpected response.';
            setModalState('error');
        }
    } catch (err) {
        document.getElementById('errorText').textContent = 'Network error — could not reach the server. ' + err.message;
        setModalState('error');
    }
}

function setModalState(state) {
    document.getElementById('modalLoading').style.display = state === 'loading' ? 'flex'  : 'none';
    document.getElementById('modalBody').style.display    = state === 'result'  ? 'block' : 'none';
    document.getElementById('modalError').style.display   = state === 'error'   ? 'flex'  : 'none';
    document.getElementById('copyBtn').style.display      = state === 'result'  ? 'inline-flex' : 'none';
}

function copyResult() {
    const text = document.getElementById('aiResult').innerText;
    navigator.clipboard.writeText(text).then(() => {
        const btn = document.getElementById('copyBtn');
        btn.textContent = '✓ Copied!';
        setTimeout(() => btn.innerHTML = '📋 Copy Response', 2000);
    });
}

function formatAiResult(raw) {
    if (!raw) return '';

    let html = raw;

    html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
        const escaped = escapeHtml(code.trim());
        return `<pre><code class="lang-${lang}">${escaped}</code></pre>`;
    });

    html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
    html = html.replace(/^## (.+)$/gm,  '<h2>$1</h2>');
    html = html.replace(/^# (.+)$/gm,   '<h1>$1</h1>');

    html = html.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>');
    html = html.replace(/\*\*(.+?)\*\*/g,     '<strong>$1</strong>');
    html = html.replace(/\*(.+?)\*/g,          '<em>$1</em>');

    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

    html = html.replace(/\b(Critical)\b/g, '<span class="severity-critical">Critical</span>');
    html = html.replace(/\b(High)\b/g,     '<span class="severity-high">High</span>');
    html = html.replace(/\b(Medium)\b/g,   '<span class="severity-medium">Medium</span>');
    html = html.replace(/\b(Low)\b/g,      '<span class="severity-low">Low</span>');

    html = html.replace(/^[\-\*] (.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>\n?)+/g, match => `<ul>${match}</ul>`);

    html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>');

    html = html
        .split(/\n{2,}/)
        .map(block => {
            block = block.trim();
            if (!block) return '';
            // Don't wrap if it's already a block element
            if (/^<(h[1-6]|ul|ol|li|pre|blockquote)/.test(block)) return block;
            return `<p>${block.replace(/\n/g, '<br>')}</p>`;
        })
        .join('\n');

    return html;
}

function escapeHtml(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}
