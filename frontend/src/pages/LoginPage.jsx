
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, LockKeyhole, Eye, EyeOff } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import './AuthPage.css'

function LoginPage() {
    const { login } = useAuth()
    const navigate = useNavigate()

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    async function handleSubmit(e) {
        e.preventDefault()
        setError('')
        setLoading(true)

        try {
            await login(email, password)
            navigate('/find-size')
        } catch (err) {
            setError(err.message || 'Unable to log in. Please try again.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <main className="auth-page">
            <section className="auth-card">
                <div className="auth-icon">
                    <LockKeyhole size={22} />
                </div>

                <p className="auth-overline">YOUR PERFECT FIT AWAITS</p>
                <h1>Welcome back.</h1>
                <p className="auth-description">
                    Sign in to access your measurements and discover your perfect size.
                </p>

                <form onSubmit={handleSubmit} className="auth-form">
                    <label htmlFor="email">EMAIL ADDRESS</label>
                    <input
                        id="email"
                        type="email"
                        placeholder="you@example.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        autoComplete="email"
                        required
                    />

                    <label htmlFor="password">PASSWORD</label>
                    <div className="password-wrapper">
                        <input
                            id="password"
                            type={showPassword ? 'text' : 'password'}
                            placeholder="Enter your password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="current-password"
                            required
                        />

                        <button
                            type="button"
                            className="password-toggle"
                            onClick={() => setShowPassword(!showPassword)}
                            aria-label={showPassword ? 'Hide password' : 'Show password'}
                        >
                            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                        </button>
                    </div>

                    {error && <p className="auth-error">{error}</p>}

                    <button type="submit" disabled={loading}>
                        {loading ? 'SIGNING IN...' : 'SIGN IN'}
                        {!loading && <ArrowRight size={17} />}
                    </button>
                </form>

                <p className="auth-switch">
                    New to Universal Fit? <Link to="/register">Create an account</Link>
                </p>
            </section>
        </main>
    )
}

export default LoginPage;
