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
    { number: '10', title: 'Outseam', where: 'From your natural waist down the outside of your leg to your ankle.', how: 'Stand straight and measure from the top of your waistband along the outside seam line to the ankle.', tip: 'Keep the tape straight and measure both legs if needed.' },
    { number: '11', title: 'Arm circumference', where: 'Around the fullest part of your upper arm.', how: 'Let your arm hang relaxed and wrap the tape around the widest part of your upper arm without squeezing.', tip: 'Keep your muscles relaxed while measuring.' },
    { number: '12', title: 'Foot length', where: 'From the back of your heel to the tip of your longest toe.', how: 'Stand on a sheet of paper, mark the heel and longest toe, then measure the distance between the marks.', tip: 'Measure both feet and use the larger measurement.' },
]

function MeasurementDiagram({ type, label }) {
    const lines = {
        chest: <><path d="M54 93 Q100 72 146 93" /><path d="M54 93 Q100 118 146 93" /></>,
        waist: <><path d="M60 145 Q100 132 140 145" /><path d="M60 153 Q100 166 140 153" /></>,
        hips: <><path d="M48 190 Q100 174 152 190" /><path d="M48 201 Q100 217 152 201" /></>,
        shoulder: <><path d="M53 83 L147 83" /><path d="M53 76 L53 90 M147 76 L147 90" /></>,
        sleeve: <><path d="M146 84 L183 169" /><path d="M176 164 L190 173" /></>,
        inseam: <><path d="M99 218 L92 319" /><path d="M101 218 L126 319" /></>,
        thigh: <><path d="M68 222 Q84 216 99 228" /><path d="M68 236 Q84 230 99 242" /></>,
        neck: <><ellipse cx="100" cy="49" rx="19" ry="8" /></>,
        height: <><path d="M177 20 L177 320" /><path d="M169 20 L185 20 M169 320 L185 320" /></>,
        outseam: <><path d="M145 220 L154 318" /><path d="M147 220 L156 318" /></>,
        arm: <><ellipse cx="161" cy="139" rx="12" ry="8" /><path d="M147 131 Q161 139 175 131" /></>,
        foot: <><path d="M68 302 Q90 294 112 302 L151 308 Q164 311 160 320 L72 320 Q58 316 68 302 Z" /><path d="M70 296 L160 296" /><path d="M70 290 L70 302 M160 290 L160 302" /></>,
    }
    return <div className="measurement-diagram" role="img" aria-label={label + ' measurement placement diagram'}>
        <svg viewBox="0 0 210 340" xmlns="http://www.w3.org/2000/svg">
            <g className="diagram-body">
                <circle cx="100" cy="31" r="19" />
                <path d="M81 55 Q57 58 48 83 L28 151 Q25 164 38 169 L51 174 L65 119 L61 207 Q58 220 68 224 L65 315 Q64 326 78 326 L91 229 L99 225 L108 229 L117 315 Q118 326 132 326 L130 224 Q141 220 138 207 L135 119 L149 174 L162 169 Q175 164 172 151 L152 83 Q143 58 119 55 Z" />
            </g>
            <g className="diagram-measure">{lines[type]}</g>
            <g className="diagram-endpoints">{type === 'height' ? <><circle cx="177" cy="20" r="3"/><circle cx="177" cy="320" r="3"/></> : null}</g>
        </svg>
        <span>{label}</span>
    </div>
}

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
                <div className="discovery-section-heading guide-heading"><div><p className="discovery-eyebrow">MEASUREMENT CHECKLIST</p><h2>How to measure.</h2></div><span className="discovery-count">12 measurements</span></div>
                <div className="measurement-guide-grid">{guides.map(item => <article className="measurement-guide-card" key={item.number}><MeasurementDiagram type={['chest','waist','hips','shoulder','neck','sleeve','inseam','thigh','height','outseam','arm','foot'][Number(item.number) - 1]} label={item.title} /><div className="guide-card-top"><span>{item.number}</span><Ruler size={19} /></div><h3>{item.title}</h3><p className="guide-where">{item.where}</p><p>{item.how}</p><div className="guide-tip"><CheckCircle2 size={15} /><span>{item.tip}</span></div></article>)}</div>
                
                <div className="guide-cta"><div><p className="discovery-eyebrow">READY TO FIND YOUR SIZE?</p><h2>Save your measurements once.</h2><p>Use the same profile to get size recommendations across supported brands.</p></div><Link to="/find-size">Open my profile <ArrowRight size={16} /></Link></div>
            </section>
        </div>
    )
}

export default SizeGuidePage
