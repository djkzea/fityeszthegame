"""A jatek sajat hangszerei: vonos, zongora, ustdob, ora - es egy terem."""
import numpy as np

SR = 44100
rng = np.random.default_rng(7)

def t_of(dur):            return np.arange(int(dur*SR))/SR
def midi(m):              return 440.0*2**((m-69)/12)

def fft_filter(x, lo=None, hi=None, slope=2.0):
    """Lagy le- es felvagas frekvenciaban - nincs scipy, ez viszont pontos."""
    n = len(x); X = np.fft.rfft(x); f = np.fft.rfftfreq(n, 1/SR)
    g = np.ones_like(f)
    if hi:  g *= 1/np.sqrt(1+(f/hi)**(2*slope))
    if lo:  g *= 1/np.sqrt(1+(lo/np.maximum(f,1e-6))**(2*slope))
    return np.fft.irfft(X*g, n)

def env(n, attack, release, hold=None):
    """Lassu be-, lassu lecsengés, koszinusz-ivekkel."""
    a = int(attack*SR); r = int(release*SR)
    e = np.ones(n)
    a = min(a, n); r = min(r, n-a)
    e[:a] = (1-np.cos(np.linspace(0, np.pi, a)))/2
    if r: e[n-r:] = (1+np.cos(np.linspace(0, np.pi, r)))/2
    return e

def strings(freq, dur, attack=1.1, release=2.2, harmonics=14, bright=2400, detune=(-6, 7)):
    """Vonos: enyhen elhangolt fureszfogak, lassu vibratoval, letompitva."""
    t = t_of(dur); out = np.zeros_like(t)
    vib = 0.0022*np.sin(2*np.pi*4.1*t + rng.random()*6) + 0.0012*np.sin(2*np.pi*0.23*t)
    for c in detune:
        f = freq*2**(c/1200)
        ph = 2*np.pi*f*(t + np.cumsum(vib)/SR)
        for h in range(1, harmonics+1):
            if f*h > 11000: break
            out += (1/h**1.22)*np.sin(h*ph + rng.random()*6)
    out *= env(len(t), attack, release)
    return fft_filter(out, lo=55, hi=bright)/ (len(detune)*2.2)

def piano(freq, dur, vel=1.0):
    """Zongora: inharmonikus felhangok, magasak gyorsabban halnak el."""
    t = t_of(dur); out = np.zeros_like(t)
    for h in range(1, 13):
        f = freq*h*(1 + 0.00042*h*h)
        if f > 12000: break
        out += (1/h**1.55)*np.sin(2*np.pi*f*t)*np.exp(-t*(1.1 + 0.55*h))
    knock = rng.standard_normal(len(t))*np.exp(-t*140)*0.06
    return fft_filter(out*vel + knock, lo=45, hi=6500)*0.5

def timpani(freq, dur=3.2):
    """Ustdob: lecsuszo alaphang es egy kis bor."""
    t = t_of(dur)
    glide = freq*(1 + 0.16*np.exp(-t*9))
    body = np.sin(2*np.pi*np.cumsum(glide)/SR)*np.exp(-t*2.1)
    body += 0.35*np.sin(2*np.pi*np.cumsum(glide*1.58)/SR)*np.exp(-t*4.5)
    skin = rng.standard_normal(len(t))*np.exp(-t*26)*0.25
    return fft_filter(body + skin, lo=35, hi=1400)*0.9

def tick(dur=0.06):
    """Ora: egy szaraz kattanas, messzirol."""
    t = t_of(dur)
    n = rng.standard_normal(len(t))*np.exp(-t*220)
    return fft_filter(n, lo=1800, hi=4200)*0.9

def air(n, level=0.004):
    return fft_filter(rng.standard_normal(n), lo=200, hi=7000)*level

def room(x, tau=1.5, length=2.2, mix=0.34, seed=1):
    """Terem: zajbol szott lecsengés, FFT-vel osszefuzve."""
    r = np.random.default_rng(seed)
    n = int(length*SR); t = np.arange(n)/SR
    ir = r.standard_normal(n)*np.exp(-t/tau)
    ir[:int(0.012*SR)] = 0                       # eloter
    ir = fft_filter(ir, lo=90, hi=7200)
    ir /= np.sqrt((ir**2).sum())
    m = len(x)+n-1; size = 1 << (m-1).bit_length()
    wet = np.fft.irfft(np.fft.rfft(x, size)*np.fft.rfft(ir, size), size)[:len(x)]
    return (1-mix)*x + mix*wet*np.sqrt(len(ir))*0.16

def place(buf, piece, at):
    i = int(at*SR); j = min(len(buf), i+len(piece))
    if i < len(buf): buf[i:j] += piece[:j-i]

def stereo(mono, width=0.012):
    """Ket kicsit mas terem: ettol lesz szelesseg, de a kozep egyben marad."""
    l = room(mono, seed=1); r = room(mono, seed=2)
    d = int(width*SR)
    r = np.concatenate([np.zeros(d), r])[:len(l)]
    return np.stack([l, r], axis=1)

def save(path, y, peak=0.89):
    import wave
    y = y/np.abs(y).max()*peak
    y16 = np.clip(y*32767, -32768, 32767).astype('<i2')
    w = wave.open(path, 'w'); w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(y16.tobytes()); w.close()
    return y16
