import { Link } from 'react-router-dom'
import { ArrowRight, Ruler, Scan, Shirt, Sparkles, ShieldCheck, CheckCircle2, Info } from 'lucide-react'
import './HowItWorksPage.css'

const steps = [
  {
    number: '01', icon: Ruler, label: 'MEASURE ONCE',
    title: 'Create your measurement profile',
    description: 'Add your body measurements once, including chest or bust, waist, hips, shoulders and relevant length measurements. Choose centimetres or inches and set your preferred fit.',
    details: ['Use the measurement guide for help', 'Save your profile securely to your account', 'Update it whenever your measurements change'],
    note: 'Your saved profile is reused for future size recommendations.'
  },
  {
    number: '02', icon: Scan, label: 'CHOOSE A BRAND',
    title: 'Select the brand you are interested in',
    description: 'Choose from the brands currently supported by Universal Fit. Depending on the brand, you may also need to select a region or sizing system.',
    details: ['Browse supported brands', 'Check the available regions', 'See which clothing categories have size data'],
    note: 'Brand and region availability depends on the size-chart data currently available.'
  },
  {
    number: '03', icon: Shirt, label: 'CHOOSE A CATEGORY',
    title: 'Tell us what you want to size',
    description: 'Select a supported clothing category such as T-shirts, shirts, jeans, trousers or jackets. The available categories update based on the selected brand.',
    details: ['Choose only from supported categories', 'Universal Fit selects the applicable chart', 'No need to choose body or garment measurement basis yourself'],
    note: 'Some styles fit differently. A recommendation is only made when a suitable chart is available.'
  },
  {
    number: '04', icon: Sparkles, label: 'GET YOUR RECOMMENDATION',
    title: 'Understand your recommended size',
    description: 'Universal Fit compares your saved measurements with the selected brand and category size data, then provides a recommended size when the data supports a reliable result.',
    details: ['View your recommended size', 'Review confidence and fit explanation', 'Compare alternative sizes when available'],
    note: 'Recommendations are guidance, not a guarantee. Cut, fabric and personal preference can affect fit.'
  }
]

function HowItWorksPage() {
  return (
    <div className="how-it-works-page">
      <section className="hiw-hero">
        <p className="hiw-eyebrow"><span /> THE UNIVERSAL FIT PROCESS</p>
        <h1>One profile.<br /><em>Every brand, made simpler.</em></h1>
        <p className="hiw-lead">Measure once, choose a brand and clothing category, and get a size recommendation based on the chart data available.</p>
        <div className="hiw-actions">
          <Link to="/find-size" className="hiw-primary">FIND MY SIZE <ArrowRight size={16} /></Link>
          <Link to="/size-guide" className="hiw-secondary">HOW TO MEASURE <Ruler size={15} /></Link>
        </div>
        <div className="hiw-flow">
          {steps.map((step, index) => {
            const Icon = step.icon
            return <div className="hiw-flow-item" key={step.number}>
              <span>{step.number}</span><Icon size={21} /><strong>{['PROFILE', 'BRAND', 'CATEGORY', 'RESULT'][index]}</strong>
            </div>
          })}
        </div>
      </section>

      <section className="hiw-steps-section">
        <div className="hiw-heading">
          <p className="hiw-eyebrow">FOUR SIMPLE STEPS</p>
          <h2>From your measurements<br /><em>to a more confident choice.</em></h2>
        </div>
        {steps.map((step, index) => {
          const Icon = step.icon
          return <article className={index % 2 ? 'hiw-step hiw-step-reverse' : 'hiw-step'} key={step.number}>
            <div className="hiw-visual">
              <span className="hiw-number">{step.number}</span>
              <div className="hiw-orbit hiw-orbit-a" />
              <div className="hiw-orbit hiw-orbit-b" />
              <div className="hiw-icon"><Icon size={48} strokeWidth={1.15} /></div>
              <span className="hiw-visual-label">{step.label}</span>
            </div>
            <div className="hiw-copy">
              <p className="hiw-eyebrow">{step.label}</p>
              <h3>{step.title}</h3>
              <p className="hiw-description">{step.description}</p>
              <ul>{step.details.map(item => <li key={item}><CheckCircle2 size={16} />{item}</li>)}</ul>
              <div className="hiw-note"><Info size={17} /><span>{step.note}</span></div>
            </div>
          </article>
        })}
      </section>

      <section className="hiw-transparency">
        <ShieldCheck size={32} />
        <div>
          <p className="hiw-eyebrow">TRANSPARENCY MATTERS</p>
          <h2>Clear recommendations. No made-up size charts.</h2>
          <p>Size charts can vary by brand, region, category and style. Universal Fit uses the applicable chart data and makes its recommendation confidence and source-verification information visible where available. If suitable data is missing, a recommendation may not be available.</p>
        </div>
      </section>

      <section className="hiw-bottom">
        <div><p className="hiw-eyebrow">YOUR FIT JOURNEY STARTS HERE</p><h2>One profile.<br /><em>A simpler way to find your size.</em></h2></div>
        <Link to="/find-size" className="hiw-primary">START FINDING MY SIZE <ArrowRight size={16} /></Link>
      </section>
    </div>
  )
}

export default HowItWorksPage
