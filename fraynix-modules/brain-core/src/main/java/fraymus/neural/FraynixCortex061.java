package fraymus.neural;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** Selected FraynixGPT Chronos/Fly algorithms, source f81d58a07790256c07b30c0d12958748b0ec329d.
 * Full continuation codec and bounded history are workspace repairs. Role names are internal
 * numerical labels, not tool capabilities. REM synthetic waves are internal replay, never world evidence. */
    public final class FraynixCortex061 {
        static final double PHI = (1.0 + Math.sqrt(5.0)) / 2.0, TWO_PI = 2 * Math.PI, GOLDEN_ANGLE = TWO_PI / (PHI * PHI);
        static final double PHI75 = 4721424167835376.0;
        static final byte[] MAGIC = "FRAYSEAL".getBytes(StandardCharsets.US_ASCII);

        static long mix(long s) {
            long z = s + 0x9e3779b97f4a7c15L;
            z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
            z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
            return z ^ (z >>> 31);
        }
        static double signedUnit(long seed, int i) { return ((mix(seed ^ (i * 0x517cc1b727220a95L)) >>> 11) * 0x1.0p-53) * 2.0 - 1.0; }
        static long fnv(byte[] d, int off, int len) {
            long h = 0xcbf29ce484222325L;
            for (int i = off; i < off + len; i++) { h ^= (d[i] & 0xFFL); h *= 0x100000001b3L; }
            return h;
        }
        static long fnv(String s) { byte[] b = s.getBytes(StandardCharsets.UTF_8); return fnv(b, 0, b.length); }

        enum Role {
            AXIOM_ARCHITECT("symbolic_ep3_solver"), MEMORY_ARCHIVIST("akashic_query"), SECURITY_SENTINEL("act_now_4d_transcoder"),
            CODE_SYNTHESIZER("bytecode_ast_verifier"), CONTRADICTION_HUNTER("hdc_counterfactual_xor"), HARMONIC_NAVIGATOR("kuramoto_phase_tuner"),
            WASM_PACKER("binary_seal_exporter"), PREFRONTAL_ARBITER("jev_consensus_voter");
            final String tool;
            Role(String t) { tool = t; }
            static Role of(int i) { Role[] v = values(); return v[Math.floorMod(i, v.length)]; }
        }

        static final class Column {
            final String id, parent; final int gen, N; final Role role;
            final double[] B, pred; final double[][] W;
            double theta, omega, cumSurprise, lastSurprise;
            int highStress, idle, flash; boolean fossil; String seal = "";

            Column(String id, String parent, int gen, int N, Role role) {
                this.id = id; this.parent = parent; this.gen = gen; this.N = N; this.role = role;
                B = new double[N]; pred = new double[N]; W = new double[N][N];
                long h = fnv(id);
                long idSeed = h * 0x9e3779b97f4a7c15L ^ role.ordinal();
                theta = ((h & 0xFFFF) * GOLDEN_ANGLE) % TWO_PI;
                double frac = ((((h >>> 16) & 0xFF) + 1) * PHI) % 1.0;
                omega = TWO_PI * (432.0 * (0.5 + 0.5 * frac));
                for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) if (i != j) W[i][j] = signedUnit(idSeed, i * N + j) * 0.14;
                reseal();
            }

            double step(double[] in, double R, double dt) {
                double mse = 0;
                for (int i = 0; i < Math.min(N, in.length); i++) {
                    double x = in[i] * (1.0 + 0.05 * Math.cos(role.ordinal() + i)), e = x - pred[i];
                    mse += e * e;
                }
                lastSurprise = Math.cbrt(Math.max(1e-12, mse / N));
                cumSurprise = cumSurprise * 0.85 + lastSurprise * 0.15;
                highStress = cumSurprise > 0.35 ? highStress + 1 : 0;
                idle = lastSurprise < 0.008 ? idle + 1 : 0;
                if (flash > 0) flash--;
                double[] nb = new double[N];
                for (int i = 0; i < N; i++) {
                    double s = 0;
                    for (int j = 0; j < N; j++) s += W[i][j] * Math.tanh(B[j]);
                    double inp = i < in.length ? in[i] : 0;
                    nb[i] = B[i] + (dt / 90.0) * (-B[i] + s + inp);
                    pred[i] = Math.tanh(s);
                }
                double gain = Math.max(0.05, R * lastSurprise);
                for (int i = 0; i < N; i++) {
                    double post = Math.tanh(nb[i]);
                    for (int j = 0; j < N; j++) {
                        if (i == j) continue;
                        double pre = Math.tanh(B[j]);
                        W[i][j] = Math.max(-2, Math.min(2, W[i][j] + gain * 0.015 * (pre * post - 0.12 * post * post * W[i][j])));
                    }
                }
                System.arraycopy(nb, 0, B, 0, N);
                reseal();
                return lastSurprise;
            }

            void reseal() {
                ByteBuffer b = ByteBuffer.allocate(N * 8 + 20);
                b.putInt(role.ordinal()).putDouble(theta).putDouble(cumSurprise);
                for (double v : B) b.putDouble(v);
                seal = hex(hash(b.array())).substring(0, 16);
            }
            double energy() { double s = 0; for (double v : B) s += Math.abs(Math.tanh(v)); return s / N; }
            double meanAbsW() { double s = 0; for (double[] r : W) for (double v : r) s += Math.abs(v); return s / Math.max(1, N * N); }
            double phaseAlign(double psi) { return 0.5 * (1.0 + Math.cos(theta - psi)); }

            int[] ternary() {
                double g = Math.max(1e-9, meanAbsW());
                int[] p = new int[(N * N + 15) / 16];
                int idx = 0;
                for (int i = 0; i < N; i++) for (int j = 0; j < N; j++, idx++) {
                    double s = W[i][j] / g;
                    int c = s > 0.5 ? 1 : s < -0.5 ? 2 : 0;
                    p[idx >>> 4] |= c << ((idx & 15) * 2);
                }
                return p;
            }
            byte[] wBlob() { ByteBuffer b = ByteBuffer.allocate(N * N * 8); for (double[] r : W) for (double v : r) b.putDouble(v); return b.array(); }
            byte[] sBlob() { ByteBuffer b = ByteBuffer.allocate(N * 16); for (double v : B) b.putDouble(v); for (double v : pred) b.putDouble(v); return b.array(); }
            Column copy() {
                Column c = new Column(id, parent, gen, N, role);
                for (int i = 0; i < N; i++) System.arraycopy(W[i], 0, c.W[i], 0, N);
                System.arraycopy(B, 0, c.B, 0, N); System.arraycopy(pred, 0, c.pred, 0, N);
                c.theta = theta; c.omega = omega; c.cumSurprise = cumSurprise; c.lastSurprise = lastSurprise;
                c.highStress = highStress; c.idle = idle; c.flash = flash; c.fossil = fossil; c.seal = seal;
                return c;
            }
        }

        // ---- JEV two-round HDC debate ----
        static final int HW = 128;
        static long[] hdc(Column c) {
            long[] v = new long[HW];
            int NN = c.N * c.N;
            for (int b = 0; b < HW * 64; b++) {
                double t = ((b / NN) % 8 - 3.5) * 0.02;
                if (c.W[(b % NN) / c.N][(b % NN) % c.N] > t) v[b >>> 6] |= 1L << (b & 63);
            }
            return v;
        }
        static long[] rot1(long[] v) {
            long[] o = new long[HW];
            for (int i = 0; i < HW; i++) o[i] = (v[i] << 1) | (v[(i + HW - 1) % HW] >>> 63);
            return o;
        }
        static long[] bundle(List<long[]> vs) {
            long[] o = new long[HW];
            for (int w = 0; w < HW; w++) for (int bit = 0; bit < 64; bit++) {
                int cnt = 0; for (long[] v : vs) cnt += (v[w] >>> bit) & 1;
                boolean set = 2 * cnt > vs.size() || (2 * cnt == vs.size() && (w & 1) == 0);
                if (set) o[w] |= 1L << bit;
            }
            return o;
        }
        static double sim(long[] a, long[] b) { int d = 0; for (int i = 0; i < HW; i++) d += Long.bitCount(a[i] ^ b[i]); return 1.0 - d / (double) (HW * 64); }

        static final class Verdict {
            List<String> approved = new ArrayList<>(), quarantined = new ArrayList<>(); double ratio; boolean supermajority; String seal;
        }

        static Verdict debate(List<Column> cols, double meanPsi) {
            Verdict v = new Verdict();
            if (cols.isEmpty()) { v.seal = ""; return v; }
            List<long[]> hv = new ArrayList<>(); for (Column c : cols) hv.add(hdc(c));
            long[] maj = bundle(hv);
            List<long[]> surv = new ArrayList<>();
            for (int i = 0; i < cols.size(); i++) {
                double r1 = sim(hv.get(i), maj) - (cols.get(i).cumSurprise > 0.65 ? 0.25 : 0.0);
                if (r1 >= 0.51) surv.add(hv.get(i));
            }
            long[] refined = surv.isEmpty() ? maj : bundle(surv);
            long[] key = rot1(refined);
            for (int i = 0; i < cols.size(); i++) {
                Column c = cols.get(i);
                double r1 = sim(hv.get(i), maj) - (c.cumSurprise > 0.65 ? 0.25 : 0.0);
                double r2 = sim(rot1(hv.get(i)), key);
                double fin = 0.45 * r1 + 0.45 * r2 + 0.10 * c.phaseAlign(meanPsi) - (c.cumSurprise > 0.65 ? 0.25 : 0.0);
                if (fin >= 0.52 && c.cumSurprise <= 0.65) v.approved.add(c.id); else v.quarantined.add(c.id);
            }
            v.ratio = v.approved.size() / (double) cols.size();
            v.supermajority = v.ratio >= 0.80;
            ByteBuffer b = ByteBuffer.allocate(8 + HW * 8).putDouble(v.ratio);
            for (long w : maj) b.putLong(w);
            v.seal = hex(hash(b.array()));
            return v;
        }

        /** Debate-gated REM: only approved columns (plus fossils of equal N) feed the master; weak synapses are halved on a copy. */
        static Column rem(List<Column> cols, List<Column> fossils, double mult, double meanPsi, int cycle) {
            if (cols.isEmpty()) return null;
            int N = cols.get(0).N;
            Verdict vd = debate(cols, meanPsi);
            List<Column> pool = new ArrayList<>();
            for (Column c : cols) if (vd.approved.contains(c.id)) pool.add(c);
            if (fossils != null) pool.addAll(fossils.subList(0, Math.min(4, fossils.size())));
            Column om = new Column("Col-Master-Omega", "HIVE_CONSENSUS", cycle, N, Role.PREFRONTAL_ARBITER);
            om.flash = 30;
            double[][] m = new double[N][N]; double tot = 0;
            for (Column c : pool) {
                if (c.N != N) continue;
                double fw = mult * (c.energy() + 0.1) * (0.2 + c.phaseAlign(meanPsi));
                tot += fw;
                for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) {
                    double w = Math.abs(c.W[i][j]) < 0.04 ? c.W[i][j] * 0.5 : c.W[i][j];
                    m[i][j] += w * fw;
                }
            }
            double norm = Math.max(1e-9, tot);
            for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) om.W[i][j] = Math.max(-2, Math.min(2, m[i][j] / norm));
            double[] wave = new double[N];
            for (int t = 1; t <= 8; t++) {
                for (int n = 0; n < N; n++) wave[n] = Math.cos((t * n * PHI) % TWO_PI) * 0.5;
                om.step(wave, 1.0, 16.0);
            }
            return om;
        }

        // ---- the hive ----
        final List<Column> active = new ArrayList<>();
        final Map<String, Column> fossils = new LinkedHashMap<>();
        final TreeMap<Integer, String> snapshots = new TreeMap<>();
        int tick, mitosis, apoptosis, resurrection, sleeps;
        double R, psi;

        public FraynixCortex061(int cols, int neurons) {
            if(cols<1||cols>16||neurons<1||neurons>32)throw new IllegalArgumentException("Cortex dimensions out of bounds");
            for (int i = 0; i < cols; i++) active.add(new Column("Col-Genesis-" + i, "ROOT", 1, neurons, Role.of(i)));
        }

        boolean resurrect(String id) {
            if(active.size()>=17)return false;
            Column c = fossils.remove(id);
            if (c == null) return false;
            c.fossil = false; c.idle = 0; c.flash = 25; active.add(c); resurrection++;
            return true;
        }

        String snapshotAt(int t) { Map.Entry<Integer, String> e = snapshots.floorEntry(t); return e == null ? null : e.getValue(); }

        public String step(double[] in, double dt) {
            if(in.length!=active.get(0).N||!Double.isFinite(dt)||dt<=0||dt>16||tick>=1000000)throw new IllegalArgumentException("Invalid cortex step");
            for(double v:in)if(!Double.isFinite(v)||Math.abs(v)>4)throw new IllegalArgumentException("Invalid cortex input");
            tick++;
            double sc = 0, ss = 0;
            for (Column c : active) { sc += Math.cos(c.theta); ss += Math.sin(c.theta); }
            int n = Math.max(1, active.size());
            R = Math.hypot(sc / n, ss / n); psi = Math.atan2(ss / n, sc / n);
            List<Column> spawned = new ArrayList<>(), pruned = new ArrayList<>();
            for (Column c : active) {
                double d = c.omega + 1.8 * R * Math.sin(psi - c.theta);
                c.theta = ((c.theta + d * (dt / 1000.0)) % TWO_PI + TWO_PI) % TWO_PI;
                c.step(in, R, dt);
                if (c.highStress >= 3 && active.size() + spawned.size() < 16) {
                    mitosis++; c.highStress = 0; c.cumSurprise *= 0.4; c.flash = 25;
                    Role cr = Role.of(c.role.ordinal() + mitosis);
                    Column ch = new Column("Col-G" + (c.gen + 1) + "-" + mitosis, c.id, c.gen + 1, c.N, cr);
                    ch.flash = 25;
                    long seed = Double.doubleToRawLongBits(c.lastSurprise) ^ fnv(c.seal) ^ mitosis;
                    for (int i = 0; i < c.N; i++) {
                        System.arraycopy(c.W[i], 0, ch.W[i], 0, c.N);
                        ch.W[i][(int) Long.remainderUnsigned(mix(seed + i), c.N)] += signedUnit(seed, i) * 0.22;
                    }
                    ch.reseal(); spawned.add(ch);
                }
                if (!c.id.equals("Col-Master-Omega") && c.idle >= 60 && active.size() - pruned.size() > 3) { apoptosis++; pruned.add(c); }
            }
            int shunts = 0;
            for (int i = 0; i < active.size(); i++) for (int j = i + 1; j < active.size(); j++) {
                Column a = active.get(i), b = active.get(j);
                double df = Math.abs(a.theta - b.theta), pd = Math.min(df, TWO_PI - df);
                if (pd < 0.25) {
                    shunts++;
                    for (int k = 0; k < Math.min(a.N, b.N); k++) {
                        double avg = 0.5 * (a.B[k] + b.B[k]);
                        a.B[k] = a.B[k] * 0.95 + avg * 0.05; b.B[k] = b.B[k] * 0.95 + avg * 0.05;
                    }
                }
            }
            active.addAll(spawned);
            for (Column c : pruned) { active.remove(c); c.fossil = true; fossils.put(c.id, c); }
            StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "{\"tick\":%d,\"r\":%.4f,\"psi\":%.4f,\"mitosis\":%d,\"apoptosis\":%d,\"shunts\":%d,\"cols\":[",
                    tick, R, psi, mitosis, apoptosis, shunts));
            for (int i = 0; i < active.size(); i++) {
                Column c = active.get(i);
                sb.append(i > 0 ? "," : "").append(String.format(Locale.ROOT, "{\"id\":\"%s\",\"role\":\"%s\",\"theta\":%.4f,\"s\":%.4f,\"seal\":\"%s\"}",
                        c.id, c.role.name(), c.theta, c.cumSurprise, c.seal));
            }
            String js = sb.append("]}").toString();
            snapshots.put(tick, js);
            while(snapshots.size()>64)snapshots.pollFirstEntry();
            while(fossils.size()>32)fossils.remove(fossils.keySet().iterator().next());
            if (tick % 25 == 0) consolidate();
            return js;
        }

        Column consolidate() {
            sleeps++;
            Column om = rem(active, new ArrayList<>(fossils.values()), 1.0, psi, sleeps);
            if (om == null) return null;
            active.removeIf(c -> c.id.equals("Col-Master-Omega"));
            for (Column c : active) {
                for (int i = 0; i < c.N; i++) for (int j = 0; j < c.N; j++) c.W[i][j] = c.W[i][j] * 0.85 + om.W[i][j] * 0.15;
                c.reseal();
            }
            active.add(0, om);
            return om;
        }

        public String digest() {
            return hex(hash(checkpoint()));
        }


    public int ticks(){return tick;}
    public int columns(){return active.size();}
    public int fossils(){return fossils.size();}
    public int neurons(){return active.get(0).N;}
    public double coherence(){return R;}
    public double[] project(double[] input){
        if(input.length!=neurons())throw new IllegalArgumentException("Feature dimensions");
        for(double x:input)if(!Double.isFinite(x)||Math.abs(x)>4)throw new IllegalArgumentException("Invalid feature");
        double[] out=new double[neurons()];
        for(Column c:active)for(int i=0;i<out.length;i++){
            double v=c.B[i];for(int j=0;j<input.length;j++)v+=c.W[i][j]*input[j];
            out[i]+=Math.tanh(v)/active.size();
        }
        return out;
    }
    public static double[] fly(double[] input,int size){
        if(input.length<1||input.length>64||size<1||size>128)throw new IllegalArgumentException("Encoder dimensions");
        for(double x:input)if(!Double.isFinite(x)||Math.abs(x)>4)throw new IllegalArgumentException("Encoder input");
        double[] raw=new double[size]; Integer[] order=new Integer[size];
        for(int k=0;k<size;k++){order[k]=k;for(int i=0;i<input.length;i++){
            double h=(((k+1)*31+(i+1)*17)*PHI)%1.0;
            raw[k]+=h>.75?(h-.5)*2*input[i]:0;
        }raw[k]=Math.max(0,raw[k]);}
        Arrays.sort(order,(a,b)->{int cmp=Double.compare(raw[b],raw[a]);return cmp==0?Integer.compare(a,b):cmp;});
        double[] out=new double[size];
        for(int k=0;k<Math.max(1,size/10);k++)if(raw[order[k]]>0)out[order[k]]=Math.tanh(raw[order[k]]);
        return out;
    }
    public static final int MAX_BYTES=2_000_000, VERSION=61;
    private static final int HEADER=0x46435258;
    public static byte[] hash(byte[] b){try{return MessageDigest.getInstance("SHA-256").digest(b);}catch(Exception e){throw new IllegalStateException(e);}}
    public static String hex(byte[] b){return HexFormat.of().formatHex(b);}
    /** Full bounded continuation. No clock, lineage or lifecycle field is inferred on restore. */
    public byte[] checkpoint(){try{
        var bytes=new ByteArrayOutputStream();var o=new DataOutputStream(bytes);
        o.writeInt(HEADER);o.writeInt(VERSION);o.writeInt(neurons());
        for(int v:new int[]{tick,mitosis,apoptosis,resurrection,sleeps})o.writeInt(v);
        o.writeDouble(R);o.writeDouble(psi);
        o.writeInt(active.size());for(Column c:active)write(o,c);
        o.writeInt(fossils.size());for(Column c:fossils.values())write(o,c);
        o.writeInt(snapshots.size());for(var e:snapshots.entrySet()){o.writeInt(e.getKey());o.writeUTF(e.getValue());}
        o.flush();byte[] body=bytes.toByteArray();o.write(hash(body));o.flush();
        if(bytes.size()>MAX_BYTES)throw new IllegalStateException("Cortex checkpoint budget");
        return bytes.toByteArray();
    }catch(IOException e){throw new IllegalStateException(e);}}
    private static void write(DataOutputStream o,Column c)throws IOException{
        o.writeUTF(c.id);o.writeUTF(c.parent);o.writeInt(c.gen);o.writeInt(c.role.ordinal());
        for(double v:new double[]{c.theta,c.omega,c.cumSurprise,c.lastSurprise})o.writeDouble(v);
        o.writeInt(c.highStress);o.writeInt(c.idle);o.writeInt(c.flash);o.writeBoolean(c.fossil);o.writeUTF(c.seal);
        for(double[] row:c.W)for(double v:row)o.writeDouble(v);
        for(double v:c.B)o.writeDouble(v);for(double v:c.pred)o.writeDouble(v);
    }
    private static int count(DataInputStream i,int low,int high)throws IOException{
        int n=i.readInt();if(n<low||n>high)throw new IOException("Checkpoint count out of range");return n;
    }
    private static double finite(DataInputStream i,double limit)throws IOException{
        double v=i.readDouble();if(!Double.isFinite(v)||Math.abs(v)>limit)throw new IOException("Checkpoint number out of range");return v;
    }
    private static String text(DataInputStream i,int max)throws IOException{String s=i.readUTF();if(s.length()>max)throw new IOException("Checkpoint string budget");return s;}
    private static Column read(DataInputStream i,int n)throws IOException{
        String id=text(i,128),parent=text(i,128);if(id.isEmpty()||parent.isEmpty())throw new IOException("Empty lineage");
        var c=new Column(id,parent,count(i,1,1000001),n,Role.values()[count(i,0,Role.values().length-1)]);
        c.theta=finite(i,TWO_PI);c.omega=finite(i,10000);c.cumSurprise=finite(i,100);c.lastSurprise=finite(i,100);
        c.highStress=count(i,0,1000000);c.idle=count(i,0,1000000);c.flash=count(i,0,1000000);c.fossil=i.readBoolean();c.seal=text(i,16);
        if(!c.seal.matches("[0-9a-f]{16}"))throw new IOException("Invalid operational fingerprint");
        for(int a=0;a<n;a++)for(int b=0;b<n;b++)c.W[a][b]=finite(i,2.3);
        for(int a=0;a<n;a++)c.B[a]=finite(i,100);for(int a=0;a<n;a++)c.pred[a]=finite(i,1);
        return c;
    }
    public static FraynixCortex061 restore(byte[] all)throws IOException{
        if(all==null||all.length<80||all.length>MAX_BYTES)throw new IOException("Checkpoint size rejected");
        int len=all.length-32;byte[] body=Arrays.copyOf(all,len);
        if(!MessageDigest.isEqual(hash(body),Arrays.copyOfRange(all,len,all.length)))throw new IOException("Checkpoint checksum rejected");
        try(var i=new DataInputStream(new ByteArrayInputStream(body))){
            if(i.readInt()!=HEADER||i.readInt()!=VERSION)throw new IOException("Unsupported cortex checkpoint version");
            int n=count(i,1,32);var h=new FraynixCortex061(1,n);h.active.clear();
            h.tick=count(i,0,1000000);h.mitosis=count(i,0,1000000);h.apoptosis=count(i,0,1000000);h.resurrection=count(i,0,1000000);h.sleeps=count(i,0,1000000);
            h.R=finite(i,1.000001);h.psi=finite(i,Math.PI);var ids=new HashSet<String>();
            int nc=count(i,1,17);for(int c=0;c<nc;c++){Column col=read(i,n);if(col.fossil||!ids.add(col.id))throw new IOException("Invalid active column");h.active.add(col);}
            nc=count(i,0,32);for(int c=0;c<nc;c++){Column col=read(i,n);if(!col.fossil||!ids.add(col.id))throw new IOException("Invalid fossil");h.fossils.put(col.id,col);}
            nc=count(i,0,64);int prev=0;for(int c=0;c<nc;c++){int t=count(i,prev+1,h.tick);h.snapshots.put(t,text(i,16000));prev=t;}
            if(i.available()!=0)throw new IOException("Trailing checkpoint bytes");return h;
        }catch(RuntimeException e){throw new IOException("Malformed checkpoint",e);}
    }
}
