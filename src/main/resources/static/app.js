// ═══════════════════════════════════════════════════════════════
// AI Cost Intelligence — Dashboard Application Logic v2.0
// ═══════════════════════════════════════════════════════════════

const API_BASE = 'http://localhost:8080/api';

let barChart = null;
let pieChart = null;
let agentPerfChart = null;

// ── Format Currency ──
function formatCurrency(val) {
    if (val === undefined || val === null) return '₹0';
    if (val >= 100000) return '₹' + (val / 100000).toFixed(2) + 'L';
    if (val >= 1000) return '₹' + val.toLocaleString('en-IN');
    return '₹' + val.toFixed(0);
}

// ── Run Analysis ──
async function runAnalysis() {
    const btn = document.getElementById('runAnalysisBtn');
    btn.classList.add('loading');
    btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M4 2l10 6-10 6V2z" fill="currentColor"/></svg> Analyzing...';

    try {
        // First load transactions
        const txnRes = await fetch(`${API_BASE}/data`);
        const transactions = await txnRes.json();
        renderTransactions(transactions);

        // Run analysis pipeline
        const analysisRes = await fetch(`${API_BASE}/analyze`, { method: 'POST' });
        const result = await analysisRes.json();

        // Update dashboard
        updateSummaryCards(result.impact);
        renderROIMetrics(result);
        renderIssues(result);
        renderActions(result.actions || []);
        renderPlaybooks(result.playbooks || []);
        renderApprovals(result.approvals || []);
        renderCharts(result.impact);
        renderAgentPerformanceChart(result.agentActivity);
        renderAgentActivity(result.agentActivity);
        renderAuditLog(result.auditLog);

    } catch (error) {
        console.error('Analysis failed:', error);
        alert('Failed to connect to backend. Make sure the Spring Boot server is running on port 8080.');
    } finally {
        btn.classList.remove('loading');
        btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M4 2l10 6-10 6V2z" fill="currentColor"/></svg> Run Analysis';
    }
}

// ── Update Summary Cards ──
function updateSummaryCards(impact) {
    if (!impact) return;
    animateValue('totalCost', impact.totalCost);
    animateValue('wasteDetected', impact.totalWaste);
    animateValue('savingsGenerated', impact.totalSavings);
    animateValue('yearlySavings', impact.yearlySavings);
}

function animateValue(elementId, target) {
    const el = document.getElementById(elementId);
    const duration = 1200;
    const start = performance.now();
    const startVal = 0;

    function update(now) {
        const elapsed = now - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        const current = startVal + (target - startVal) * eased;
        el.textContent = formatCurrency(Math.round(current));
        if (progress < 1) requestAnimationFrame(update);
    }
    requestAnimationFrame(update);
}

// ── Render ROI Metrics ──
function renderROIMetrics(result) {
    const actions = result.actions || [];
    const playbooks = result.playbooks || [];
    const approvals = result.approvals || [];
    const impact = result.impact || {};

    const executed = actions.filter(a => a.status === 'EXECUTED').length;
    const pending = approvals.filter(a => a.status === 'PENDING').length;
    const executedSavings = actions.filter(a => a.status === 'EXECUTED')
        .reduce((sum, a) => sum + a.savings, 0);
    const pendingSavings = actions.filter(a => a.status === 'PENDING_APPROVAL')
        .reduce((sum, a) => sum + a.savings, 0);
    const efficiency = impact.totalCost > 0
        ? ((impact.totalSavings / impact.totalCost) * 100).toFixed(1) : '0';

    document.getElementById('efficiencyPct').textContent = efficiency + '%';
    document.getElementById('executedCount').textContent = executed;
    document.getElementById('pendingCount').textContent = pending;
    document.getElementById('playbookCount').textContent = playbooks.length;
    document.getElementById('costRecovered').textContent = formatCurrency(executedSavings);
    document.getElementById('pendingSavingsVal').textContent = formatCurrency(pendingSavings);
}

// ── Render Transactions Table ──
function renderTransactions(transactions) {
    const tbody = document.getElementById('transactionsBody');
    document.getElementById('txnCount').textContent = transactions.length;

    if (!transactions.length) {
        tbody.innerHTML = '<tr><td colspan="8" class="empty-state">No transactions found</td></tr>';
        return;
    }

    tbody.innerHTML = transactions.map(t => `
        <tr>
            <td style="color: var(--accent-indigo); font-weight: 600;">${t.id}</td>
            <td style="color: var(--text-primary); font-weight: 500;">${t.vendor}</td>
            <td class="amount-positive">${formatCurrency(t.amount)}</td>
            <td>${t.date}</td>
            <td>${t.resourceUsage > 0 ? renderUsageBar(t.resourceUsage) : '—'}</td>
            <td>${t.expectedAmount > 0 ? formatCurrency(t.expectedAmount) : '—'}</td>
            <td>${t.actualAmount > 0 ? formatCurrency(t.actualAmount) : '—'}</td>
            <td><span class="status-badge status-${t.status.toLowerCase()}">${t.status}</span></td>
        </tr>
    `).join('');
}

function renderUsageBar(usage) {
    const color = usage < 40 ? 'var(--accent-rose)' : usage < 70 ? 'var(--accent-amber)' : 'var(--accent-emerald)';
    return `<div style="display:flex;align-items:center;gap:8px;">
        <div style="width:60px;height:6px;background:rgba(255,255,255,0.06);border-radius:3px;overflow:hidden;">
            <div style="width:${usage}%;height:100%;background:${color};border-radius:3px;transition:width 0.8s ease;"></div>
        </div>
        <span style="font-size:0.75rem;color:${color};font-weight:600;">${usage}%</span>
    </div>`;
}

// ── Render Issues Table ──
function renderIssues(result) {
    const tbody = document.getElementById('issuesBody');

    fetchResults().then(data => {
        const allIssues = data.issues || [];
        document.getElementById('issueCount').textContent = allIssues.length;

        if (!allIssues.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No issues detected</td></tr>';
            return;
        }

        tbody.innerHTML = allIssues.map(i => `
            <tr>
                <td style="color: var(--accent-indigo); font-weight: 600;">${i.id}</td>
                <td><span style="color: var(--text-primary); font-weight: 500;">${formatIssueType(i.type)}</span></td>
                <td><span class="severity severity-${i.severity.toLowerCase()}">${i.severity}</span></td>
                <td style="max-width: 300px;">${i.description}</td>
                <td style="color: var(--accent-cyan); font-weight: 500;">${i.agent}</td>
                <td style="max-width: 200px; font-size: 0.78rem;">${i.suggestedAction}</td>
                <td class="amount-positive">${formatCurrency(i.potentialSavings)}</td>
            </tr>
        `).join('');
    });
}

async function fetchResults() {
    try {
        const res = await fetch(`${API_BASE}/results`);
        return await res.json();
    } catch {
        return { issues: [], actions: [] };
    }
}

function formatIssueType(type) {
    return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

// ── Render Corrective Actions Table ──
function renderActions(actions) {
    const tbody = document.getElementById('actionsBody');
    document.getElementById('actionCount').textContent = actions.length;

    if (!actions.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No actions taken yet</td></tr>';
        return;
    }

    tbody.innerHTML = actions.map(a => `
        <tr>
            <td style="color: var(--accent-indigo); font-weight: 600;">${a.id}</td>
            <td><span class="action-type-badge">${formatIssueType(a.type)}</span></td>
            <td style="max-width: 400px; font-size: 0.78rem;">${a.description}</td>
            <td><span class="action-status action-status-${a.status.toLowerCase().replace(/_/g, '-')}">${a.status.replace(/_/g, ' ')}</span></td>
            <td class="amount-positive">${formatCurrency(a.savings)}</td>
        </tr>
    `).join('');
}

// ── Render Playbooks ──
function renderPlaybooks(playbooks) {
    const container = document.getElementById('playbooksContainer');
    document.getElementById('playbookBadge').textContent = playbooks.length;

    if (!playbooks.length) {
        container.innerHTML = '<div class="empty-state">No playbooks generated yet</div>';
        return;
    }

    container.innerHTML = `
        <div class="playbooks-toolbar">
            <button class="btn-toggle-all" onclick="toggleAllPlaybooks()">
                <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                    <path d="M2 4l5 5 5-5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span id="toggleAllLabel">Collapse All</span>
            </button>
            <span class="playbooks-summary">${playbooks.length} playbooks · ${formatCurrency(playbooks.reduce((s, p) => s + p.estimatedSavings, 0))} total potential savings</span>
        </div>
    ` + playbooks.map((pb, i) => `
        <div class="playbook-card" style="animation: fadeInUp ${0.2 + i * 0.06}s ease both;">
            <div class="playbook-header" onclick="togglePlaybook('pb-${i}')">
                <div class="playbook-meta">
                    <span class="severity severity-${pb.priority.toLowerCase()}">${pb.priority}</span>
                    <span class="playbook-agent">${pb.agent}</span>
                    <span class="playbook-status playbook-st-${pb.status.toLowerCase()}">${pb.status}</span>
                </div>
                <h3 class="playbook-title">${pb.title}</h3>
                <div class="playbook-savings">
                    <span class="playbook-savings-value">${formatCurrency(pb.estimatedSavings)}</span>
                    <span class="playbook-savings-label">est. savings</span>
                </div>
                <svg class="playbook-chevron playbook-chevron-open" id="chevron-pb-${i}" width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
            </div>
            <div class="playbook-body playbook-body-open" id="pb-${i}">
                <div class="playbook-steps">
                    ${pb.steps.map((step, si) => `
                        <div class="playbook-step">
                            <span class="step-number">${si + 1}</span>
                            <span class="step-text">${step.replace(/^\d+\.\s*/, '')}</span>
                        </div>
                    `).join('')}
                </div>
                <div class="cost-math-box">
                    <div class="cost-math-label">📊 Cost Math Breakdown</div>
                    <div class="cost-math-content">${pb.costMath.replace(/\|/g, '<br>')}</div>
                </div>
                ${pb.triggerIssueId ? `<div class="playbook-trigger">Triggered by issue: <span class="trigger-id">${pb.triggerIssueId}</span></div>` : ''}
            </div>
        </div>
    `).join('');
}

let allPlaybooksExpanded = true;

function togglePlaybook(id) {
    const body = document.getElementById(id);
    const chevron = document.getElementById('chevron-' + id);
    const isOpen = body.classList.contains('playbook-body-open');

    if (isOpen) {
        body.classList.remove('playbook-body-open');
        body.classList.add('playbook-body-closed');
        chevron.classList.remove('playbook-chevron-open');
        chevron.classList.add('playbook-chevron-closed');
    } else {
        body.classList.remove('playbook-body-closed');
        body.classList.add('playbook-body-open');
        chevron.classList.remove('playbook-chevron-closed');
        chevron.classList.add('playbook-chevron-open');
    }
}

function toggleAllPlaybooks() {
    const bodies = document.querySelectorAll('.playbook-body');
    const chevrons = document.querySelectorAll('.playbook-chevron');
    allPlaybooksExpanded = !allPlaybooksExpanded;

    bodies.forEach(body => {
        if (allPlaybooksExpanded) {
            body.classList.remove('playbook-body-closed');
            body.classList.add('playbook-body-open');
        } else {
            body.classList.remove('playbook-body-open');
            body.classList.add('playbook-body-closed');
        }
    });

    chevrons.forEach(chevron => {
        if (allPlaybooksExpanded) {
            chevron.classList.remove('playbook-chevron-closed');
            chevron.classList.add('playbook-chevron-open');
        } else {
            chevron.classList.remove('playbook-chevron-open');
            chevron.classList.add('playbook-chevron-closed');
        }
    });

    document.getElementById('toggleAllLabel').textContent = allPlaybooksExpanded ? 'Collapse All' : 'Expand All';
}

// ── Render Approval Queue ──
function renderApprovals(approvals) {
    const container = document.getElementById('approvalsContainer');
    const pending = approvals.filter(a => a.status === 'PENDING').length;
    document.getElementById('approvalBadge').textContent = pending;

    if (!approvals.length) {
        container.innerHTML = '<div class="empty-state">No pending approvals</div>';
        return;
    }

    container.innerHTML = approvals.map((a, i) => `
        <div class="approval-card approval-${a.status.toLowerCase()}" id="approval-${a.id}">
            <div class="approval-header">
                <div class="approval-meta">
                    <span class="risk-badge risk-${a.riskLevel.toLowerCase()}">${a.riskLevel} RISK</span>
                    <span class="approval-agent">${a.agent}</span>
                    <span class="approval-id">${a.id}</span>
                </div>
                <span class="approval-status-badge approval-st-${a.status.toLowerCase()}">${a.status}</span>
            </div>
            <p class="approval-desc">${truncateText(a.description, 150)}</p>
            <div class="approval-footer">
                <span class="approval-impact">Impact: ${formatCurrency(a.estimatedImpact)}</span>
                <span class="approval-time">${a.createdAt}</span>
                ${a.status === 'PENDING' ? `
                    <div class="approval-actions">
                        <button class="btn-approve" onclick="handleApproval('${a.id}', 'approve')">✓ Approve</button>
                        <button class="btn-reject" onclick="handleApproval('${a.id}', 'reject')">✕ Reject</button>
                    </div>
                ` : `<span class="approval-resolved">Resolved: ${a.resolvedAt || '—'}</span>`}
            </div>
        </div>
    `).join('');
}

function truncateText(text, maxLen) {
    if (!text || text.length <= maxLen) return text;
    return text.substring(0, maxLen) + '…';
}

async function handleApproval(id, action) {
    try {
        const res = await fetch(`${API_BASE}/approvals/${id}/${action}`, { method: 'POST' });
        if (res.ok) {
            const updated = await res.json();
            // Refresh approvals
            const allRes = await fetch(`${API_BASE}/approvals`);
            const allApprovals = await allRes.json();
            renderApprovals(allApprovals);

            // Flash feedback
            const card = document.getElementById(`approval-${id}`);
            if (card) {
                card.classList.add('approval-flash');
                setTimeout(() => card.classList.remove('approval-flash'), 600);
            }
        }
    } catch (error) {
        console.error('Approval action failed:', error);
    }
}

// ── Render Charts ──
function renderCharts(impact) {
    if (!impact) return;
    renderBarChart(impact);
    renderPieChart(impact);
}

function renderBarChart(impact) {
    const ctx = document.getElementById('barChart').getContext('2d');

    if (barChart) barChart.destroy();

    barChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Total Cost', 'Waste Detected', 'Savings Generated', 'Monthly Savings', 'Yearly Projection'],
            datasets: [{
                label: 'Amount (₹)',
                data: [impact.totalCost, impact.totalWaste, impact.totalSavings, impact.monthlySavings, impact.yearlySavings],
                backgroundColor: [
                    'rgba(34, 211, 238, 0.7)',
                    'rgba(244, 63, 94, 0.7)',
                    'rgba(16, 185, 129, 0.7)',
                    'rgba(168, 85, 247, 0.7)',
                    'rgba(99, 102, 241, 0.7)'
                ],
                borderColor: ['#22d3ee', '#f43f5e', '#10b981', '#a855f7', '#6366f1'],
                borderWidth: 1.5,
                borderRadius: 8,
                borderSkipped: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: 'rgba(10, 11, 20, 0.95)',
                    titleColor: '#f1f5f9',
                    bodyColor: '#94a3b8',
                    borderColor: 'rgba(99, 102, 241, 0.2)',
                    borderWidth: 1,
                    padding: 14,
                    cornerRadius: 10,
                    callbacks: {
                        label: (ctx) => ' ₹' + ctx.parsed.y.toLocaleString('en-IN')
                    }
                }
            },
            scales: {
                x: {
                    ticks: { color: '#64748b', font: { size: 11, family: 'Inter' } },
                    grid: { display: false },
                    border: { display: false }
                },
                y: {
                    ticks: {
                        color: '#64748b',
                        font: { size: 11, family: 'Inter' },
                        callback: (v) => '₹' + (v >= 100000 ? (v/100000).toFixed(1) + 'L' : v.toLocaleString('en-IN'))
                    },
                    grid: { color: 'rgba(255,255,255,0.04)' },
                    border: { display: false }
                }
            },
            animation: { duration: 1200, easing: 'easeOutQuart' }
        }
    });
}

function renderPieChart(impact) {
    const ctx = document.getElementById('pieChart').getContext('2d');
    const breakdown = impact.wasteBreakdown || {};

    if (pieChart) pieChart.destroy();

    const labels = Object.keys(breakdown).map(k => formatIssueType(k));
    const data = Object.values(breakdown);

    const colors = [
        'rgba(244, 63, 94, 0.8)', 'rgba(245, 158, 11, 0.8)',
        'rgba(99, 102, 241, 0.8)', 'rgba(168, 85, 247, 0.8)',
        'rgba(16, 185, 129, 0.8)', 'rgba(34, 211, 238, 0.8)',
        'rgba(251, 146, 60, 0.8)', 'rgba(139, 92, 246, 0.8)'
    ];

    pieChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors.slice(0, data.length),
                borderColor: 'rgba(10, 11, 20, 0.8)',
                borderWidth: 3,
                hoverOffset: 12
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '55%',
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        color: '#94a3b8',
                        font: { size: 11, family: 'Inter' },
                        padding: 16,
                        usePointStyle: true,
                        pointStyleWidth: 10
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(10, 11, 20, 0.95)',
                    titleColor: '#f1f5f9',
                    bodyColor: '#94a3b8',
                    borderColor: 'rgba(99, 102, 241, 0.2)',
                    borderWidth: 1,
                    padding: 14,
                    cornerRadius: 10,
                    callbacks: {
                        label: (ctx) => ' ₹' + ctx.parsed.toLocaleString('en-IN')
                    }
                }
            },
            animation: { animateRotate: true, duration: 1200, easing: 'easeOutQuart' }
        }
    });
}

// ── Render Agent Performance Chart ──
function renderAgentPerformanceChart(agentActivity) {
    if (!agentActivity || !Object.keys(agentActivity).length) return;

    const ctx = document.getElementById('agentPerfChart').getContext('2d');
    if (agentPerfChart) agentPerfChart.destroy();

    const agents = Object.keys(agentActivity);
    const issues = agents.map(a => agentActivity[a].issuesDetected || 0);
    const actions = agents.map(a => agentActivity[a].actionsExecuted || 0);
    const savings = agents.map(a => agentActivity[a].totalSavings || 0);
    const playbooks = agents.map(a => agentActivity[a].playbooksGenerated || 0);

    agentPerfChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: agents,
            datasets: [
                {
                    label: 'Issues',
                    data: issues,
                    backgroundColor: 'rgba(244, 63, 94, 0.7)',
                    borderColor: '#f43f5e',
                    borderWidth: 1,
                    borderRadius: 6,
                    borderSkipped: false
                },
                {
                    label: 'Actions',
                    data: actions,
                    backgroundColor: 'rgba(99, 102, 241, 0.7)',
                    borderColor: '#6366f1',
                    borderWidth: 1,
                    borderRadius: 6,
                    borderSkipped: false
                },
                {
                    label: 'Playbooks',
                    data: playbooks,
                    backgroundColor: 'rgba(168, 85, 247, 0.7)',
                    borderColor: '#a855f7',
                    borderWidth: 1,
                    borderRadius: 6,
                    borderSkipped: false
                }
            ]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        color: '#94a3b8',
                        font: { size: 11, family: 'Inter' },
                        usePointStyle: true,
                        pointStyleWidth: 10,
                        padding: 20
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(10, 11, 20, 0.95)',
                    titleColor: '#f1f5f9',
                    bodyColor: '#94a3b8',
                    borderColor: 'rgba(99, 102, 241, 0.2)',
                    borderWidth: 1,
                    padding: 14,
                    cornerRadius: 10
                }
            },
            scales: {
                x: {
                    ticks: { color: '#64748b', font: { size: 11, family: 'Inter' } },
                    grid: { color: 'rgba(255,255,255,0.04)' },
                    border: { display: false }
                },
                y: {
                    ticks: {
                        color: '#94a3b8',
                        font: { size: 11, family: 'Inter', weight: 600 }
                    },
                    grid: { display: false },
                    border: { display: false }
                }
            },
            animation: { duration: 1200, easing: 'easeOutQuart' }
        }
    });
}

// ── Render Agent Activity ──
function renderAgentActivity(agentActivity) {
    const container = document.getElementById('agentActivity');
    if (!agentActivity || !Object.keys(agentActivity).length) {
        container.innerHTML = '<div class="empty-state">No agent activity</div>';
        return;
    }

    const agentColors = ['#6366f1', '#a855f7', '#22d3ee', '#10b981'];

    container.innerHTML = Object.entries(agentActivity).map(([name, data], i) => `
        <div class="agent-card" style="animation: fadeInUp ${0.4 + i * 0.15}s ease forwards;">
            <div class="agent-name" style="--agent-color: ${agentColors[i % 4]}">
                ${name}
            </div>
            <div class="agent-stats">
                <div class="agent-stat">
                    <span class="agent-stat-value" style="color: ${agentColors[i % 4]}">${data.issuesDetected}</span>
                    <span class="agent-stat-label">Issues</span>
                </div>
                <div class="agent-stat">
                    <span class="agent-stat-value" style="color: var(--accent-amber)">${data.actionsExecuted}</span>
                    <span class="agent-stat-label">Actions</span>
                </div>
                <div class="agent-stat">
                    <span class="agent-stat-value" style="color: var(--accent-purple)">${data.playbooksGenerated || 0}</span>
                    <span class="agent-stat-label">Playbooks</span>
                </div>
                <div class="agent-stat">
                    <span class="agent-stat-value" style="color: var(--accent-emerald)">${formatCurrency(data.totalSavings)}</span>
                    <span class="agent-stat-label">Savings</span>
                </div>
            </div>
        </div>
    `).join('');
}

// ── Render Audit Log ──
function renderAuditLog(auditLog) {
    const container = document.getElementById('auditLog');
    if (!auditLog || !auditLog.length) {
        container.innerHTML = '<div class="empty-state">No audit entries</div>';
        return;
    }

    container.innerHTML = auditLog.map(entry => `
        <div class="audit-entry">
            <span class="audit-step">[${entry.step}]</span>
            <div class="audit-content">
                <span class="audit-agent">${entry.agent}</span>: ${entry.message}
                <br><span class="audit-time">${entry.timestamp}</span>
            </div>
        </div>
    `).join('');

    container.scrollTop = container.scrollHeight;
}

// ── Initialize ──
document.addEventListener('DOMContentLoaded', () => {
    console.log('AI Cost Intelligence Dashboard v2.0 initialized');
});
