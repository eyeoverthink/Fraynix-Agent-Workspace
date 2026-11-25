"""Create independent AVI, PNG and WAV inputs for the timeline acceptance tests."""
from pathlib import Path
import math
import struct
import subprocess
import sys
import wave
import zlib

out = Path(sys.argv[1])
out.mkdir(parents=True, exist_ok=True)
w = h = 64

def chunk(tag,data):
    return struct.pack(">I",len(data))+tag+data+struct.pack(">I",zlib.crc32(tag+data))

rows = []
for y in range(h):
    row = bytearray([0])
    for x in range(w):
        row.extend((218,51,184,255) if 14<=x<50 and 16<=y<46 else (0,0,0,0))
    rows.append(bytes(row))
header = struct.pack(">2I5B",w,h,8,6,0,0,0)
(out/"overlay.png").write_bytes(b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",header)
                                +chunk(b"IDAT",zlib.compress(b"".join(rows)))+chunk(b"IEND",b""))

with wave.open(str(out/"cue.wav"),"wb") as sound:
    sound.setparams((1,2,24000,0,"NONE","not compressed"))
    samples = [round(14500*math.sin(2*math.pi*330*i/24000)+6500*math.sin(2*math.pi*510*i/24000))
               for i in range(24000)]
    sound.writeframes(struct.pack("<"+"h"*len(samples),*samples))

subprocess.run(["ffmpeg","-v","error","-y",
                "-f","lavfi","-i","color=c=0xe54536:s=640x360:r=24:d=0.333333",
                "-f","lavfi","-i","color=c=0x1577d5:s=640x360:r=24:d=0.333333",
                "-filter_complex","[0:v][1:v]concat=n=2:v=1:a=0[v]","-map","[v]",
                "-an","-c:v","mjpeg","-q:v","2","-pix_fmt","yuvj420p","-threads","1",
                str(out/"two-colors.avi")],check=True)
