import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { UserRound, Menu, X, ArrowRight } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import './Navbar.css'

const links = [
  { label: 'HOW IT WORKS', path: '/how-it-works' },
  { label: 'BRANDS', path: '/brands' },
  { label: 'FIND MY SIZE', path: '/find-size' },
]

function Navbar() {
  const { isAuthenticated, isAdmin } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const closeMenu = () => setMenuOpen(false)

  return (
    <header className="navbar uf-navbar">
      <Link to="/" className="navbar-logo uf-navbar-logo" onClick={closeMenu}>
        <span className="uf-logo-mark"><i /><i /><i /></span>
        <span className="uf-logo-word">UNIVERSAL <b>FIT</b></span>
        <small>THE SCIENCE OF PERFECT FIT</small>
      </Link>
      <nav className={menuOpen ? 'navbar-links is-open' : 'navbar-links'}>
        {links.map(link => <NavLink key={link.path} to={link.path} onClick={closeMenu} className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}>{link.label}</NavLink>)}
        {isAdmin && <NavLink to="/admin" onClick={closeMenu} className={({isActive}) => isActive ? 'nav-link active' : 'nav-link'}>ADMIN</NavLink>}
      </nav>
      <div className="navbar-actions">
        {!isAuthenticated ? <>
          <Link to="/login" className="login-link">LOG IN</Link>
          <Link to="/register" className="profile-button">Create profile <ArrowRight size={14} /></Link>
        </> : <Link to="/account" className="account-status"><UserRound size={15} /> ACCOUNT</Link>}
      </div>
      <button className="mobile-menu" aria-label={menuOpen ? 'Close menu' : 'Open menu'} aria-expanded={menuOpen} onClick={() => setMenuOpen(value => !value)}>
        {menuOpen ? <X size={21} /> : <Menu size={21} />}
      </button>
    </header>
  )
}

export default Navbar
