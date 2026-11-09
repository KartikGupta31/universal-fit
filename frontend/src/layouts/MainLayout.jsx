
import { Outlet } from 'react-router-dom'
import Navbar from '../components/Navbar'

function MainLayout() {
    return (
        <>
            <Navbar />
            <main>
                <Outlet />
            </main>
            <footer className="site-footer">
                <div className="footer-brand">
                    UNIVERSAL<span>FIT</span>
                </div>
                <p>ONE BODY. EVERY BRAND. THE PERFECT FIT.</p>
                <small>© 2026 UNIVERSAL FIT. ALL RIGHTS RESERVED.</small>
            </footer>
        </>
    )
}

export default MainLayout;
