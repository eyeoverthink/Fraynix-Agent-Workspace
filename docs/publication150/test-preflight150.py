import copy,unittest
from preflight150 import validate
def fixture():
 frames=[{'imageSha256':str(i),'width':640,'height':480,'originalBox':[300 if i<65 else 260,180,40,120]} for i in range(1,96)]
 source={'eligible':True,'donor':[1,24],'evaluation':[25,95],'selectedReviewedSourceFrames':list(range(1,96)),'event':{'hiddenStart':43,'returnFrame':65},'ranges':[{'start':1,'end':42,'visibility':'visible'},{'start':43,'end':64,'visibility':'occluded'},{'start':65,'end':95,'visibility':'visible'}],'otherBoxes':{str(i):[[.7,.3,.78,.6]] for i in range(25,96)},'lookalike':{'distinctPhysicalObject':True,'similarityRationale':'Separate similarly shaped red comparison object'}}
 return source,{'frames':frames}
class Preflight150(unittest.TestCase):
 def test_complete_fixed_window_passes(self):
  s,i=fixture();v=validate(s,i);self.assertEqual(v['evaluationFrames'],71);self.assertEqual(v['fullyHiddenFrames'],22)
 def test_wrong_source_boundaries_and_trimmed_window_refuse(self):
  for k,v in [('donor',[2,25]),('evaluation',[26,95]),('evaluation',[25,94]),('eligible',False)]:
   s,i=fixture();s[k]=v
   with self.assertRaises(AssertionError):validate(s,i)
 def test_unreviewed_original_and_duplicate_source_refuse(self):
  s,i=fixture();s['selectedReviewedSourceFrames'].pop()
  with self.assertRaises(AssertionError):validate(s,i)
  s,i=fixture();i['frames'][70]['imageSha256']=i['frames'][1]['imageSha256']
  with self.assertRaises(AssertionError):validate(s,i)
 def test_hidden_partial_and_uncertain_donor_refuse(self):
  for v in ['partial','occluded','uncertain']:
   s,i=fixture();s['ranges'][0]['visibility']=v
   with self.assertRaises(AssertionError):validate(s,i)
 def test_incomplete_cover_and_missing_clear_return_refuse(self):
  for index,v in [(1,'partial'),(2,'partial'),(2,'uncertain')]:
   s,i=fixture();s['ranges'][index]['visibility']=v
   with self.assertRaises(AssertionError):validate(s,i)
 def test_too_short_or_long_cover_refuses(self):
  for start in [49,3]:
   s,i=fixture();s['event']['hiddenStart']=start
   with self.assertRaises(AssertionError):validate(s,i)
 def test_small_movement_and_collapsed_geometry_refuse(self):
  for x,w in [(299,40),(260,0)]:
   s,i=fixture();i['frames'][64]['originalBox']=[x,180,w,120]
   with self.assertRaises(AssertionError):validate(s,i)
 def test_absent_or_overlapping_distractor_refuses(self):
  s,i=fixture();s['otherBoxes']={}
  with self.assertRaises(AssertionError):validate(s,i)
  s,i=fixture();s['otherBoxes']['65']=[[260/640,180/480,300/640,300/480]]
  with self.assertRaises(AssertionError):validate(s,i)
 def test_over_capacity_and_missing_labels_refuse(self):
  s,i=fixture();i['frames']+=copy.deepcopy(i['frames'][:30]);s['evaluation']=[25,125]
  with self.assertRaises(AssertionError):validate(s,i)
  s,i=fixture();s['ranges'][1]['start']=44
  with self.assertRaises(AssertionError):validate(s,i)
 def test_brief_reveal_transition_is_retained_and_scored_not_trimmed(self):
  s,i=fixture();s['event']['hiddenEnd']=63;s['ranges'][1]['end']=62;s['ranges'].append({'start':63,'end':64,'visibility':'partial'})
  v=validate(s,i);self.assertEqual(v['fullyHiddenFrames'],20);self.assertEqual(v['returnTransitionFrames'],2);self.assertEqual(v['evaluationFrames'],71)
if __name__=='__main__':unittest.main()
