import { Link } from 'react-router-dom'
import { ArrowRight, CheckCircle2, Info, Ruler, Shirt } from 'lucide-react'
import './DiscoveryPages.css'

const guides = [
    { number: '01', title: 'Chest / bust', where: 'Around the fullest part of your chest or bust.', how: 'Keep the tape level under your arms and across your back. Breathe normally; do not pull the tape tight.', tip: 'Keep the tape parallel to the floor.' },
    { number: '02', title: 'Waist', where: 'Around your natural waist, usually the narrowest part of your torso.', how: 'Stand relaxed and wrap the tape around your waist without holding your breath or pulling your stomach in.', tip: 'Measure against the body, not over thick clothing.' },
    { number: '03', title: 'Hips', where: 'Around the fullest part of your hips and seat.', how: 'Stand with feet together and keep the tape level all the way around.', tip: 'Check in a mirror to keep the tape horizontal.' },
    { number: '04', title: 'Shoulder width', where: 'From one shoulder point to the other across your upper back.', how: 'Find the points where your shoulders meet your arms. Measure straight across between them.', tip: 'Ask someone to help for a more accurate result.' },
    { number: '05', title: 'Neck', where: 'Around the base of your neck.', how: 'Place the tape where a shirt collar normally sits. Leave a little room for comfort.', tip: 'Do not measure high up near your jaw.' },
    { number: '06', title: 'Sleeve length', where: 'From the shoulder point to your wrist.', how: 'Keep your arm slightly bent and measure along the outside of your arm.', tip: 'Use the same arm position each time.' },
    { number: '07', title: 'Inseam', where: 'From the top of the inner thigh to the ankle.', how: 'Measure along the inside of your leg. A well-fitting pair of trousers can also be used as a reference.', tip: 'Stand naturally; do not stretch the tape.' },
    { number: '08', title: 'Thigh', where: 'Around the fullest part of one upper thigh.', how: 'Keep the tape snug but not tight, with your weight evenly distributed.', tip: 'Measure both sides if they differ noticeably.' },
    { number: '09', title: 'Height', where: 'From the floor to the top of your head.', how: 'Stand barefoot against a wall, looking straight ahead. Mark the wall and measure from the floor.', tip: 'Use a flat floor and keep your heels down.' },
]

function SizeGuidePage() {
    return (
        <div className="discovery-page">
            <section className="discovery-hero guide-hero">
                <p className="discovery-eyebrow">A BETTER FIT STARTS WITH ACCURATE MEASUREMENTS</p>
                <h1>Measure once.<br /><em>Fit with confidence.</em></h1>
                <p>Use a soft measuring tape, take measurements over light clothing, and save them to your Universal Fit profile. You can update them whenever your body changes.</p>
                <div className="discovery-hero-meta"><span><Ruler size={16} /> Soft tape measure</span><span><CheckCircle2 size={16} /> Simple step-by-step guide</span></div>
            </section>
            <section className="discovery-content guide-content">
                <div className="guide-intro"><div><p className="discovery-eyebrow">BEFORE YOU START</p><h2>Get ready to measure.</h2><p>Stand naturally, keep the tape comfortably snug, and record each measurement without compressing your body. Ask someone to help with shoulder and back measurements.</p></div><div className="guide-tool-card"><Ruler size={30} /><strong>What you'll need</strong><span>A flexible measuring tape</span><span>A mirror or a helper</span><span>A notepad or your profile page</span></div></div>
                <div className="guide-unit-note"><Info size={18} /><p><strong>Choose one unit and stay consistent.</strong> Universal Fit supports centimetres (cm) and inches (in). Your profile stores the selected unit; don't mix units in the same measurement set.</p></div>
                <div className="discovery-section-heading guide-heading"><div><p className="discovery-eyebrow">MEASUREMENT CHECKLIST</p><h2>How to measure.</h2></div><span className="discovery-count">09 essentials</span></div>
                <div className="measurement-guide-grid">{guides.map(item => <article className="measurement-guide-card" key={item.number}><div className="guide-card-top"><span>{item.number}</span><Ruler size={19} /></div><h3>{item.title}</h3><p className="guide-where">{item.where}</p><p>{item.how}</p><div className="guide-tip"><CheckCircle2 size={15} /><span>{item.tip}</span></div></article>)}</div>
                <div className="guide-extra-measurements"><Shirt size={22} /><div><h3>Other useful measurements</h3><p>For some clothing categories, your profile may also include outseam, arm circumference, and foot length. Add these when the measurement fields are available in your profile.</p></div></div>
                <div className="guide-cta"><div><p className="discovery-eyebrow">READY TO FIND YOUR SIZE?</p><h2>Save your measurements once.</h2><p>Use the same profile to get size recommendations across supported brands.</p></div><Link to="/find-size">Open my profile <ArrowRight size={16} /></Link></div>
            </section>
        </div>
    )
}

export default SizeGuidePage
