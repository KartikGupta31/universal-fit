import { useEffect, useState } from 'react'
import { getAdminDashboard, updateFeedbackModeration } from '../services/adminService'
import './AdminDashboardPage.css'

const feedbackLabels = {
    PERFECT_FIT: 'Perfect fit',
    TOO_TIGHT: 'Too tight',
    TOO_LOOSE: 'Too loose',
    TOO_SHORT: 'Too short',
    TOO_LONG: 'Too long',
}

function Metric({ label, value, detail }) {
    return <article className="admin-metric">
        <span>{label}</span>
        <strong>{Number(value || 0).toLocaleString()}</strong>
        <small>{detail}</small>
    </article>
}

function AdminDashboardPage() {
    const [data, setData] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [updatingId, setUpdatingId] = useState('')

    async function loadDashboard() {
        setError('')
        try {
            setData(await getAdminDashboard())
        } catch (err) {
            setError(err.message || 'Could not load the admin dashboard.')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { loadDashboard() }, [])

    async function moderate(id, status) {
        setUpdatingId(id)
        setError('')
        try {
            await updateFeedbackModeration(id, status)
            await loadDashboard()
        } catch (err) {
            setError(err.message || 'Could not update feedback status.')
        } finally {
            setUpdatingId('')
        }
    }

    if (loading) return <div className="admin-page"><p>Loading admin dashboard...</p></div>
    if (error && !data) return <div className="admin-page"><p className="admin-error">{error}</p></div>
    if (!data) return null

    const feedbackTypes = Object.entries(data.feedbackBreakdown || {})
    const maxFeedback = Math.max(1, ...feedbackTypes.map(([, count]) => count))

    return <div className="admin-page">
        <header className="admin-header">
            <div>
                <p className="eyebrow">UNIVERSAL FIT · ADMINISTRATION</p>
                <h1>Dashboard</h1>
                <p>Understand fit outcomes, feedback quality and brand coverage.</p>
            </div>
            <button type="button" className="admin-refresh" onClick={loadDashboard}>Refresh data</button>
        </header>

        {error && <p className="admin-error" role="alert">{error}</p>}

        <section className="admin-metrics">
            <Metric label="Registered users" value={data.totalUsers} detail="All user accounts" />
            <Metric label="Recommendations" value={data.totalRecommendations} detail="Size results generated" />
            <Metric label="Fit feedback" value={data.totalFeedback} detail="Submitted by users" />
            <Metric label="Active brands" value={data.activeBrands} detail="Available in the catalog" />
        </section>

        <section className="admin-panels">
            <article className="admin-panel">
                <div className="admin-panel-heading">
                    <div><p className="eyebrow">USER SIGNALS</p><h2>Fit feedback breakdown</h2></div>
                    <span>{data.totalFeedback} total</span>
                </div>
                <div className="admin-bars">
                    {feedbackTypes.map(([type, count]) => <div className="admin-bar-row" key={type}>
                        <div><span>{feedbackLabels[type] || type}</span><strong>{count}</strong></div>
                        <div className="admin-bar-track"><span style={{ width: `${(count / maxFeedback) * 100}%` }} /></div>
                    </div>)}
                </div>
            </article>
            <article className="admin-panel admin-moderation-summary">
                <p className="eyebrow">FEEDBACK REVIEW</p>
                <h2>Moderation status</h2>
                <div><span>Pending review</span><strong>{data.pendingFeedback}</strong></div>
                <div><span>Approved</span><strong>{data.approvedFeedback}</strong></div>
                <div><span>Rejected</span><strong>{data.rejectedFeedback}</strong></div>
            </article>
        </section>

        <section className="admin-panel admin-recent">
            <div className="admin-panel-heading">
                <div><p className="eyebrow">LATEST SUBMISSIONS</p><h2>Recent fit feedback</h2></div>
                <span>Latest 12</span>
            </div>
            {!data.recentFeedback?.length ? <p className="admin-empty">No fit feedback has been submitted yet.</p> :
                <div className="admin-table-wrap"><table className="admin-table">
                    <thead><tr><th>Brand</th><th>Experience</th><th>Recommended</th><th>Purchased</th><th>Comment</th><th>Status</th><th>Review</th></tr></thead>
                    <tbody>{data.recentFeedback.map(item => <tr key={item.id}>
                        <td>{item.brandName}</td>
                        <td>{feedbackLabels[item.feedback] || item.feedback}</td>
                        <td>{item.recommendedSize || '—'}</td>
                        <td>{item.purchasedSize || '—'}</td>
                        <td className="admin-comment">{item.comments || '—'}</td>
                        <td><span className={`admin-status ${item.moderationStatus.toLowerCase()}`}>{item.moderationStatus}</span></td>
                        <td>{item.moderationStatus === 'PENDING' ? <div className="admin-review-actions">
                            <button disabled={updatingId === item.id} onClick={() => moderate(item.id, 'APPROVED')}>Approve</button>
                            <button disabled={updatingId === item.id} onClick={() => moderate(item.id, 'REJECTED')}>Reject</button>
                        </div> : 'Reviewed'}</td>
                    </tr>)}</tbody>
                </table></div>}
        </section>
    </div>
}

export default AdminDashboardPage
