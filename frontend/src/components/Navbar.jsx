
import { Link, NavLink } from 'react-router-dom'
import { UserRound, Menu } from 'lucide-react'
import './Navbar.css'

const links = [
    { label: 'FIND MY SIZE', path: '/find-size' },
    { label: 'BRANDS', path: '/brands' },
    { label: 'COMPARE', path: '/compare' },
    { label: 'SIZE GUIDE', path: '/size-guide' },
]

function Navbar() {
    return (
        <header className="navbar">
            <Link to="/" className="navbar-logo">
                UNIVERSAL<span>FIT</span>
                <small>THE SCIENCE OF PERFECT FIT</small>
            </Link>

            <nav className="navbar-links">
                {links.map((link) => (
                    <NavLink
                        key={link.path}
                        to={link.path}
                        className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}
                    >
                        {link.label}
                    </NavLink>
                ))}
            </nav>

            <div className="navbar-actions">
                <Link to="/login" className="login-link">LOG IN</Link>
                <Link to="/register" className="profile-button">
                    <UserRound size={16} />
                    <span>GET STARTED</span>
                </Link>
            </div>

            <button className="mobile-menu" aria-label="Menu">
                <Menu size={23} />
            </button>
        </header>
    )
}

export default Navbar;
