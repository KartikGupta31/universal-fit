import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Check, ShieldCheck, Ruler, Scan, Shirt, Sparkles } from 'lucide-react'
import { getBrands } from '../services/recommendationService'
import './HomePage.css'

const steps = [
  { number: '01', icon: Ruler, title: 'Save your measurements', text: 'Create one private body measurement profile and reuse it across supported brands.' },
  { number: '02', icon: Scan, title: 'Choose a brand', text: 'Explore the brands and sizing regions currently supported by Universal Fit.' },
  { number: '03', icon: Shirt, title: 'Select a category', text: 'Choose the clothing category you want to size. Available categories depend on the brand.' },
  { number: '04', icon: Sparkles, title: 'Get your recommendation', text: 'See your recommended size, confidence, fit explanation and available alternatives.' },
]

function Mannequin() {
  return (
    <div className="uf-mannequin-scene" role="img" aria-label="Illustration of a mannequin with chest, waist and hip measurement rings">
      <div className="uf-orbit" />
      <div className="uf-mannequin">
        <div className="uf-head" />
        <div className="uf-neck" />
        <div className="uf-torso" />
        <div className="uf-arm uf-arm-left" />
        <div className="uf-arm uf-arm-right" />
        <div className="uf-leg uf-leg-left" />
        <div className="uf-leg uf-leg-right" />
        <i className="uf-ring uf-ring-chest"><span>CHEST</span></i>
        <i className="uf-ring uf-ring-waist"><span>WAIST</span></i>
        <i className="uf-ring uf-ring-hips"><span>HIPS</span></i>
      </div>
      <div className="uf-pedestal" />
      <div className="uf-float uf-float-one"><span>MEASUREMENT PROFILE</span><strong>One profile</strong><small>Saved securely</small></div>
      <div className="uf-float uf-float-two"><span>YOUR FIT</span><strong>Brand by brand</strong><small>Chart-based sizing</small></div>
      <div className="uf-visual-caption">UF / PERSONAL FIT PROFILE</div>
    </div>
  )
}

function HomePage() {
  const [brands, setBrands] = useState([])

  useEffect(() => {
    let active = true
    getBrands(0, 8).then(data => {
      if (active) setBrands((data?.content || []).slice(0, 8))
    }).catch(() => {
      if (active) setBrands([])
    })
    return () => { active = false }
  }, [])

  return (
    <div className="uf-home">
      <section className="uf-hero">
        <div className="uf-hero-copy">
          <p className="uf-eyebrow"><span /> INTELLIGENT SIZING, SIMPLIFIED</p>
          <h1>One Profile.<br />Every Brand.<br /><em>Your Perfect Fit.</em></h1>
          <p className="uf-hero-lead">Save your measurements once and discover your recommended size across the clothing brands you love.</p>
          <div className="uf-hero-actions">
            <Link to="/find-size" className="uf-button-dark">Find My Size <ArrowRight size={16} /></Link>
            <Link to="/how-it-works" className="uf-button-outline">How It Works</Link>
          </div>
          <div className="uf-trust-row">
            <span><Check size={15} /> One saved profile</span>
            <span><ShieldCheck size={15} /> Private by design</span>
            <span><Sparkles size={15} /> Chart-based results</span>
          </div>
        </div>
        <div className="uf-hero-art"><Mannequin /></div>
      </section>

      <section className="uf-brand-strip">
        <p>EXPLORE SUPPORTED BRAND SIZING</p>
        <div>{brands.length ? brands.map(brand => <span key={brand.id}>{brand.name}</span>) : <span>Brand directory</span>}</div>
        <Link to="/brands">View directory <ArrowRight size={14} /></Link>
      </section>

      <section className="uf-section uf-process">
        <div className="uf-section-heading">
          <div><p className="uf-eyebrow"><span /> EFFORTLESS BY DESIGN</p><h2>Your size, decoded<br />in four simple steps.</h2></div>
          <p>No more guessing between inconsistent size charts. Your saved measurements are compared with the selected brand and category data.</p>
        </div>
        <div className="uf-step-grid">
          {steps.map((step, index) => {
            const Icon = step.icon
            return <article className={`uf-step-card uf-step-${index + 1}`} key={step.number}>
              <span className="uf-step-number">{step.number}</span>
              <div className="uf-step-icon"><Icon size={36} strokeWidth={1.25} /></div>
              <h3>{step.title}</h3><p>{step.text}</p>
            </article>
          })}
        </div>
      </section>

      <section className="uf-measure-section">
        <div className="uf-measure-art"><div className="uf-measure-glass"><strong>01</strong><span>PROFILE<br />MEASURE ONCE</span></div><div className="uf-chrome-sphere" /></div>
        <div className="uf-measure-copy">
          <p className="uf-eyebrow"><span /> MEASURE ONCE. USE EVERYWHERE.</p>
          <h2>Your body doesn't change between brands. <em>Their sizing does.</em></h2>
          <p>Universal Fit translates your saved body measurements into brand-specific size guidance using the chart data available for your selection.</p>
          <ul>
            <li><Check size={16} /> Your preferred fit is considered</li>
            <li><Check size={16} /> Choose from supported brands and categories</li>
            <li><Check size={16} /> See confidence and chart verification details</li>
          </ul>
          <Link to="/find-size" className="uf-text-link">Build your measurement profile <ArrowRight size={15} /></Link>
        </div>
      </section>

      <section className="uf-section uf-benefits">
        <div className="uf-section-heading"><div><p className="uf-eyebrow"><span /> WHY UNIVERSAL FIT</p><h2>Confidence in every choice.</h2></div></div>
        <div className="uf-benefit-grid">
          <article><span>01</span><h3>Fewer fit surprises</h3><p>Understand the recommended size before choosing a brand.</p></article>
          <article><span>02</span><h3>Less time comparing</h3><p>Use one measurement profile instead of repeatedly decoding charts.</p></article>
          <article><span>03</span><h3>Transparent data</h3><p>See when chart data is verified and when a category is unavailable.</p></article>
          <article><span>04</span><h3>You stay in control</h3><p>Your measurements are tied to your account and managed with your consent.</p></article>
        </div>
      </section>

      <section className="uf-final-cta">
        <div className="uf-cta-orbit" />
        <p className="uf-eyebrow">YOUR FIT, EVERYWHERE</p>
        <h2>Stop guessing.<br /><em>Start knowing.</em></h2>
        <p>Create your Universal Fit measurement profile and find your size with confidence.</p>
        <Link to="/find-size" className="uf-button-light">Find My Size <ArrowRight size={16} /></Link>
      </section>
    </div>
  )
}

export default HomePage
