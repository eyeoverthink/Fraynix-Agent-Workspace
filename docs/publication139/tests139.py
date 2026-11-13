from pathlib import Path
base=Path('workspace-engine/src/main/java/fraymus/workspace');p=base/'SpatialEvidence139.java';s=p.read_text(encoding='utf8');s=s.replace('boolean eligible=p.variance>.0002&&rival<.92;', '''double bgVariance=median(background.stream().map(q->q.variance).toList()),colorGap=0;for(int k=0;k<3;k++){final int channel=k;colorGap+=Math.abs(p.mean[k]-median(background.stream().map(q->q.mean[channel]).toList()))/3;}
            boolean distinct=colorGap>.04||p.variance>1.8*Math.max(.0002,bgVariance)||bgVariance>1.8*Math.max(.0002,p.variance);
            boolean eligible=p.variance>.0002&&rival<.92&&distinct;''');p.write_text(s,encoding='utf8')
p=Path('workspace-engine/src/test/java/fraymus/workspace/ForegroundGuard139Test.java');s=Path('workspace-engine/src/test/java/fraymus/workspace/DriftGuard138Test.java').read_text(encoding='utf8').replace('DriftGuard138','ForegroundGuard139').replace('DriftAudit138','ForegroundAudit139').replace('drift138','drift139').replace('drift-guard-138','foreground-guard-139')
pos=s.rfind('}');s=s[:pos]+'''
 static BufferedImage covered(int columns){
  var image=scene(0,false,false,false);var other=new Random(9139);for(int y=0;y<120;y++)for(int x=0;x<90;x++){int q=other.nextInt(70);if(x<columns)image.setRGB(40+x,35+y,((20+q/2)<<16)|((150+q/2)<<8)|(20+q/2));}return image;
 }
 @Test public void gradualSameColorCoverCannotUseColorAgreementAsSpatialProof(){
  var first=covered(0);var base=new FlowTracker("body","object",first,seed());var old=new DriftGuard138("body","object",first,seed(),true);var guarded=new ForegroundGuard139("body","object",first,seed(),true);int warnings=0,oldAccepted=0;
  for(int i=1;i<=12;i++){
   var frame=covered(Math.min(90,i*9));var b=base.update(frame);var o=old.update(frame);var g=guarded.update(frame);var m=g.getJSONObject("drift139");
   if(i>=10&&!b.isNull("box")&&!o.isNull("box")){oldAccepted++;if(g.isNull("box")&&m.getBoolean("suspect"))warnings++;}
  }
  assertTrue("Control must still accept the fully replaced same-color object",oldAccepted>0);assertTrue("Spatial structure must expose accepted replacement",warnings>0);
 }
 @Test public void persistentPartLocationsMeasureGradualBoxMisalignment(){
  var evidence=new SpatialEvidence139(scene(0,false,false,false),seed());JSONObject last=null;
  for(int i=1;i<=12;i++)last=evidence.assess(scene(i*2,false,false,false),seed());
  assertTrue(last.toString(),last.getBoolean("supported"));assertTrue(last.toString(),last.getDouble("alignmentError")>.18);assertTrue(last.getJSONArray("candidateReasons").toString().contains("Seed parts"));
  var inferred=PixelTracker.Box.from(last.getJSONArray("witnessBox"));assertEquals(seed().x()+24/320.,inferred.x(),.025);assertEquals(0,last.getInt("automaticAdmissions"));
 }
 @Test public void uniformColorDoesNotInventTexturedPartEvidence(){
  var image=new BufferedImage(320,220,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new java.awt.Color(40,90,40));g.fillRect(0,0,320,220);g.dispose();
  var evidence=new SpatialEvidence139(image,seed());var result=evidence.assess(image,seed());assertEquals(0,result.getInt("eligibleParts"));assertFalse(result.getBoolean("supported"));assertTrue(result.getJSONArray("candidateReasons").isEmpty());
 }
}
''';p.write_text(s,encoding='utf8')
