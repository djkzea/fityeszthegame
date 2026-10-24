import sys; sys.path.insert(0,'.')
import numpy as np
from synth import *

LOOP = 16.0
CH = [(45, [52, 57, 60], 71), (41, [48, 53, 57], 64),
      (38, [53, 57, 62], 64), (40, [56, 59, 62], 64)]
PIANO = [(0.6, 69), (4.8, 72), (8.7, 74), (12.5, 71)]

events = []
for i, (bass, chord, shine) in enumerate(CH):
    at = i*4.0
    events.append((at, strings(midi(bass), 4.6, attack=1.3, release=2.8, bright=2200)*0.55))
    events.append((at, np.sin(2*np.pi*midi(bass-12)*t_of(4.6))*env(int(4.6*SR), 1.3, 2.8)*0.22))
    for note in chord:
        events.append((at, strings(midi(note), 4.8, attack=1.5, release=3.0, bright=4400)*0.40))
    events.append((at, strings(midi(shine), 4.4, attack=2.2, release=2.6, bright=7000)*0.15))
for at, note in PIANO:
    events.append((at, piano(midi(note), 3.4, vel=0.55)))
one_tick = tick()*0.07
for k in range(int(LOOP)):
    events.append((float(k), one_tick))

n = int(2*LOOP*SR); buf = np.zeros(n)
for at, wave in events:
    place(buf, wave, at); place(buf, wave, at + LOOP)
buf += np.tile(air(int(LOOP*SR), 0.006), 2)

menu = stereo(buf)[int(LOOP*SR):int(2*LOOP*SR)]
save('menu-loop.wav', menu)

m = menu.mean(1); m = m/np.abs(m).max()
print(f"MENU seam={abs(m[0]-m[-1])*32768:.0f} (median {np.median(np.abs(np.diff(m)))*32768:.0f})")
