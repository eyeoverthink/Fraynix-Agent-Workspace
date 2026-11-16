"""Public views omit camera RGB samples; original private receipts stay unchanged."""
import hashlib,json

FEATURE_KEYS={'descriptor','pixels','coarse'}
sha=lambda b:hashlib.sha256(b).hexdigest()

def public_view(raw):
    value=json.loads(raw);omitted=[]
    def visit(v,path):
        if isinstance(v,dict):
            out={}
            for key,item in v.items():
                location=path+'/'+key
                if key in FEATURE_KEYS and isinstance(item,list):
                    encoded=json.dumps(item,separators=(',',':'),allow_nan=False).encode()
                    marker={'privateSourceFeatures':True,'values':len(item),'canonicalArraySha256':sha(encoded)}
                    omitted.append({'path':location,**marker});out[key]=marker
                else:out[key]=visit(item,location)
            return out
        if isinstance(v,list):return [visit(x,path+'/'+str(i)) for i,x in enumerate(v)]
        return v
    result=visit(value,'')
    if omitted:
        assert isinstance(result,dict)
        result['_publication149']={'scope':'Derived public view. RGB sample arrays remain in the unchanged private receipt. Counts, decisions and source hashes are retained. This view cannot substitute for original evidence in verification.','privateOriginalBytes':len(raw),'privateOriginalSha256':sha(raw),'omittedFeatureArrays':omitted}
    return result,omitted

def public_bytes(raw):
    value,omitted=public_view(raw)
    return ((json.dumps(value,indent=2,ensure_ascii=False)+'\n').encode() if omitted else raw),omitted

def check_public(v):
    if isinstance(v,dict):
        assert not ({'image','base64','imageData','dataUrl'}&set(v)),'Private image payload'
        for key,item in v.items():
            assert not(key in FEATURE_KEYS and isinstance(item,list)),'Private sampled image colors'
            check_public(item)
    elif isinstance(v,list):
        for item in v:check_public(item)
    elif isinstance(v,str):assert 'data:image/' not in v

if __name__=='__main__':
    from pathlib import Path
    original={'passed':True,'correct':4,'box':[.1,.2,.3,.4],'reference':{'descriptor':[.1,.4],'structure149':{'pixels':[.2,.3,.5],'coarse':[.6]}}}
    raw=(json.dumps(original)+'\n').encode();public,omitted=public_bytes(raw);v=json.loads(public)
    assert len(omitted)==3 and v['correct']==4 and v['box']==original['box']
    assert v['_publication149']['privateOriginalSha256']==sha(raw)
    assert original['reference']['descriptor']==[.1,.4]
    assert public_bytes(raw)[0]==public
    check_public(v)
    try:check_public(original)
    except AssertionError:pass
    else:raise AssertionError('Unredacted camera samples accepted')
    Path('build/public-evidence149-test.json').write_bytes((json.dumps({'passed':True,'checks':['RGB descriptors omitted with hashes','Source bytes unchanged','Measurements and geometry preserved','Deterministic public view','Unredacted samples refused']},indent=2)+'\n').encode())
