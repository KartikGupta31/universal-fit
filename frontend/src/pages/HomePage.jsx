
import { Link } from 'react-router-dom'
import {
    ArrowRight,
    Ruler,
    Scan,
    Shirt,
    Sparkles,
    CheckCircle2,
} from 'lucide-react'
import './HomePage.css'

const steps = [
    {
        number: '01',
        icon: Ruler,
        title: 'MEASURE ONCE',
        description: 'Create your personal body measurement profile with simple guided steps.',
    },
    {
        number: '02',
        icon: Scan,
        title: 'CHOOSE A BRAND',
        description: 'Explore different clothing brands and their unique sizing systems.',
    },
    {
        number: '03',
        icon: Shirt,
        title: 'CHOOSE A CATEGORY',
        description: 'Choose T-shirts, shirts, jeans, trousers or another clothing category.',
    },
    {
        number: '04',
        icon: Sparkles,
        title: 'GET YOUR FIT',
        description: 'Receive your recommended size and alternative fit options.',
    },
]

function HomePage() {
    return (
        <div className="home-page">
            <section className="home-hero">
                <div className="hero-copy">
                    <div className="hero-label">
                        <span className="hero-label-line" />
                        SMARTER SIZING STARTS HERE
                    </div>

                    <h1>
                        One body.
                        <br />
                        Every brand.
                        <br />
                        <em>The perfect fit.</em>
                    </h1>

                    <p className="hero-description">
                        Your body doesn't change with every brand. Your size shouldn't
                        either. Create one measurement profile and discover your
                        perfect size across clothing brands.
                    </p>

                    <div className="hero-buttons">
                        <Link to="/find-size" className="hero-primary">
                            FIND MY SIZE <ArrowRight size={16} />
                        </Link>
                        <Link to="/how-it-works" className="hero-secondary">
                            HOW IT WORKS
                        </Link>
                    </div>

                    <div className="hero-trust">
                        <CheckCircle2 size={15} />
                        ONE PROFILE. MULTIPLE BRANDS. BETTER FIT.
                    </div>
                </div>

                <div className="hero-visual">
                    <img
                        src="https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200"
                        alt="Fashion models representing different clothing styles"
                    />
                    <div className="hero-image-caption">
                        <span>YOUR BODY IS UNIQUE.</span>
                        <span>YOUR FIT SHOULD BE TOO.</span>
                    </div>
                    <div className="hero-floating-card">
                        <div className="floating-icon">
                            <Scan size={19} />
                        </div>
                        <div>
                            <strong>YOUR FIT PROFILE</strong>
                            <span>One profile for every brand</span>
                        </div>
                        <CheckCircle2 size={18} className="floating-check" />
                    </div>
                </div>
            </section>

            <section className="home-intro">
                <p className="section-overline">INTRODUCING UNIVERSAL FIT</p>
                <h2>
                    Stop guessing.
                    <br />
                    <em>Start fitting.</em>
                </h2>
                <p>
                    Different brands. Different size charts. One intelligent
                    measurement profile that helps you find the right size.
                </p>
            </section>

            <section className="how-section" id="how-it-works">
                <div className="section-heading">
                    <div>
                        <p className="section-overline">SIMPLE BY DESIGN</p>
                        <h2>How it works.</h2>
                    </div>
                    <Link to="/how-it-works">
                        EXPLORE THE PROCESS <ArrowRight size={15} />
                    </Link>
                </div>

                <div className="steps-grid">
                    {steps.map((step) => {
                        const Icon = step.icon

                        return (
                            <article className="step-card" key={step.number}>
                                <div className="step-top">
                                    <span>{step.number}</span>
                                    <Icon size={23} strokeWidth={1.5} />
                                </div>
                                <h3>{step.title}</h3>
                                <p>{step.description}</p>
                            </article>
                        )
                    })}
                </div>
            </section>

            <section className="home-cta">
                <div>
                    <p className="section-overline">YOUR FIT JOURNEY STARTS HERE</p>
                    <h2>One measurement profile.<br /><em>Endless possibilities.</em></h2>
                    <p>
                        Save your measurements once. Use them to discover your
                        recommended size across brands.
                    </p>
                    <Link to="/register" className="hero-primary">
                        CREATE MY PROFILE <ArrowRight size={16} />
                    </Link>
                </div>
                <div className="cta-mark">UF.</div>
            </section>
        </div>
    )
}

export default HomePage;
