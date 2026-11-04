/** Standalone JDK probe of the exact matrix supplied in agi-fraynix-4.txt.
 * This does not install a codec or certify a different Golay implementation.
 * Run: java scripts/ProbeSuppliedEncoder060.java
 */
class ProbeSuppliedEncoder060 {
    public static void main(String[] args) {
        int[] rows={4094,3811,3525,2955,1815,3629,3163,2231,367,733,1465,2929};
        int minimum=25, witness=0, word=0;
        for(int message=1;message<4096;message++) {
            int parity=0;
            for(int bit=0;bit<12;bit++)if((message&(1<<(11-bit)))!=0)parity^=rows[bit];
            int encoded=(message<<12)|(parity&4095),weight=Integer.bitCount(encoded);
            if(weight<minimum){minimum=weight;witness=message;word=encoded;}
        }
        if(minimum!=7||witness!=1025||word!=0x401592)throw new AssertionError("Supplied-matrix probe differs");
        System.out.printf("{\"messagesEnumerated\":4095,\"minimumNonzeroCodewordWeight\":%d,\"message\":%d,\"codewordHex\":\"0x%x\",\"scope\":\"Exact supplied linear encoder; not decoder or hardware certification\"}%n",minimum,witness,word);
    }
}
