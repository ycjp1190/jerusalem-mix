(function () {
  "use strict";
  const COLORS={red:"#d73f49",yellow:"#dfb633",green:"#42b766",skyblue:"#38b9d9",cyan:"#38b9d9",blue:"#3575ca",purple:"#8b5ac8",pink:"#d45b9e",magenta:"#d45b9e",orange:"#df7b2e",off:"#626b76"};
  const names=["KICK","SNARE","HAT","TOM 1","TOM 2","BASS","E.GTR","A.GTR","KEY L","KEY R","LEAD","VOCAL 1","VOCAL 2","VOCAL 3","PASTOR","MC"];
  const colorNames=["Red","Yellow","Green","SkyBlue","Blue","Purple","Pink","Orange"];

  function channel(index,stereo=false){return{index,stereo,name:index<names.length?names[index]:(stereo?`ST ${index-71}`:`CH ${index+1}`),color:colorNames[index%8],fader:index<16?-600-(index%4)*250:-1200,on:true,gain:0,phantom:false,pan:0,peqOn:true,mixLevels:Array(24).fill(-32768),mixOn:Array(24).fill(false),eq:[{freq:800,gain:0,q:700},{freq:4000,gain:0,q:1000},{freq:25000,gain:0,q:1000},{freq:100000,gain:0,q:700}]};}
  const demoChannels=[...Array.from({length:72},(_,i)=>channel(i)),...Array.from({length:8},(_,i)=>channel(72+i,true))];
  let state={platform:"web",connection:"DEMO",connectionDetail:"DESIGN DEMO",controlEnabled:false,experimentalEnabled:false,bank:0,selectedChannel:0,selectedMix:0,sendsOnFader:false,channels:demoChannels};
  const native=window.JerusalemNative||null;
  const Core=window.JerusalemCore;
  const $=id=>document.getElementById(id);
  const strips=$("strips"),banks=$("banks"),mixModal=$("mixModal"),detailModal=$("detailModal");

  function invoke(method,...args){if(native&&typeof native[method]==="function")return native[method](...args);return null;}
  const clamp=Core.clamp,levelRatio=Core.levelRatio,db=Core.formatDb;
  function color(name){return COLORS[String(name||"").toLowerCase()]||COLORS.blue;}
  function textColor(hex){const n=parseInt(hex.slice(1),16);const r=n>>16,g=n>>8&255,b=n&255;return .2126*r+.7152*g+.0722*b>150?"#091018":"#fff";}
  function number(ch){return ch.stereo?`ST ${ch.index-71}`:`CH ${ch.index+1}`;}

  function render(){
    document.body.classList.toggle("native",state.platform==="android");
    $("statusText").textContent=state.connectionDetail||"DESIGN DEMO";
    $("platformText").textContent=state.platform==="android"?"ANDROID · LOCAL UI":"WEB · 콘솔 미연결";
    $("statusDot").className=`status-dot ${String(state.connection).toLowerCase()}`;
    $("mixButton").classList.toggle("hidden",!state.sendsOnFader);
    $("mixButton").textContent=`MIX ${state.selectedMix+1}`;
    $("sofButton").textContent=state.sendsOnFader?"EXIT SENDS":"SENDS ON FADER";
    $("sofButton").classList.toggle("active",state.sendsOnFader);
    renderStrips();renderBanks();
    if(!detailModal.hidden)renderDetail();
  }

  function renderStrips(){
    strips.replaceChildren();const start=state.bank*8;
    state.channels.slice(start,start+8).forEach(ch=>{
      const value=state.sendsOnFader?ch.mixLevels[state.selectedMix]:ch.fader;
      const on=state.sendsOnFader?ch.mixOn[state.selectedMix]:ch.on;
      const el=document.createElement("article");el.className=`strip${ch.index===state.selectedChannel?" selected":""}${state.sendsOnFader?" sends":""}`;
      const c=color(ch.color);el.style.setProperty("--ch-color",c);el.style.setProperty("--ch-text",textColor(c));el.style.setProperty("--level",levelRatio(value));
      el.innerHTML=`<div class="color-line"></div><button class="channel-name" type="button">${escapeHtml(ch.name)}</button><div class="channel-number">${number(ch)}</div><div class="fader-zone"><div class="fader-track"></div><div class="fader-scale">${[10,0,-10,-20,-40,-60].map(x=>`<i data-db="${x>0?"+":""}${x}"></i>`).join("")}</div><div class="fader-cap"></div><input class="fader" type="range" min="-6000" max="1000" step="10" value="${value<=-32000?-6000:value}" aria-label="${escapeHtml(ch.name)} fader"></div><output class="level-value">${db(value)} dB</output><button class="on-button ${on?"":"off"}" type="button">${state.sendsOnFader?(on?"SEND ON":"SEND OFF"):(on?"MUTE":"MUTED")}</button>`;
      el.querySelector(".channel-name").addEventListener("click",()=>selectChannel(ch.index));
      const slider=el.querySelector(".fader"),cap=el.querySelector(".fader-cap"),out=el.querySelector(".level-value");
      const set=(finished)=>{const v=Number(slider.value);if(state.sendsOnFader)ch.mixLevels[state.selectedMix]=v;else ch.fader=v;cap.parentElement.parentElement.style.setProperty("--level",levelRatio(v));out.textContent=`${db(v)} dB`;invoke("setFader",ch.index,v,finished);};
      slider.addEventListener("input",()=>set(false));slider.addEventListener("change",()=>set(true));
      el.querySelector(".on-button").addEventListener("click",()=>{if(state.sendsOnFader){ch.mixOn[state.selectedMix]=!ch.mixOn[state.selectedMix];invoke("setSendOn",ch.index,state.selectedMix,ch.mixOn[state.selectedMix]);}else{ch.on=!ch.on;invoke("setChannelOn",ch.index,ch.on);}renderStrips();});
      strips.append(el);
    });
  }

  function renderBanks(){banks.replaceChildren();for(let i=0;i<10;i++){const b=document.createElement("button");b.type="button";b.className=i===state.bank?"active":"";b.textContent=i===9?"ST 1–8":`${i*8+1}–${i*8+8}`;b.addEventListener("click",()=>{state.bank=i;invoke("setBank",i);render();});banks.append(b);}}
  function selectChannel(index){state.selectedChannel=index;invoke("selectChannel",index);render();}

  function openMix(){mixModal.hidden=false;const grid=$("mixGrid");grid.replaceChildren();for(let i=0;i<24;i++){const b=document.createElement("button");b.type="button";b.className=i===state.selectedMix?"active":"";b.textContent=`MIX ${i+1}`;b.addEventListener("click",()=>{state.selectedMix=i;invoke("setMix",i);mixModal.hidden=true;render();});grid.append(b);}}
  function openDetail(){detailModal.hidden=false;renderDetail();}
  function renderDetail(){
    const ch=state.channels[state.selectedChannel];if(!ch)return;$("detailTitle").textContent=`${number(ch)} · ${ch.name}`;
    bindRange("gainSlider",ch.gain,v=>`${v>=0?"+":""}${(v/100).toFixed(1)} dB`,v=>{ch.gain=v;invoke("setGain",ch.index,v)},"gainValue");
    bindRange("panSlider",ch.pan,Core.panText,v=>{ch.pan=v;invoke("setPan",ch.index,v)},"panValue");
    $("peqOn").checked=ch.peqOn;$("peqOn").onchange=e=>{ch.peqOn=e.target.checked;invoke("setPeqOn",ch.index,ch.peqOn);drawEq(ch)};
    renderEqBands(ch);drawEq(ch);
  }
  function bindRange(id,value,format,onInput,outputId){const input=$(id),out=$(outputId);input.value=value;out.value=format(value);input.oninput=()=>{const v=Number(input.value);out.value=format(v);onInput(v);};}
  function renderEqBands(ch){const root=$("eqBands");root.replaceChildren();const bandColors=["#f0c443","#57d886","#44bff0","#dc6bb1"];ch.eq.forEach((band,i)=>{const card=document.createElement("div");card.className="eq-band";card.style.setProperty("--band",bandColors[i]);card.innerHTML=`<strong>BAND ${i+1}</strong>${eqControl("FREQ",i,"freq",200,200000,band.freq)}${eqControl("GAIN",i,"gain",-1800,1800,band.gain)}${eqControl("Q",i,"q",100,16000,band.q)}`;card.querySelectorAll("input").forEach(input=>input.addEventListener("input",()=>{const key=input.dataset.key,v=Number(input.value);band[key]=v;input.previousElementSibling.value=eqValue(key,v);invoke("setEq",ch.index,i,key==="freq"?"Freq":key==="gain"?"Gain":"Q",v);drawEq(ch);}));root.append(card);});}
  function eqControl(label,i,key,min,max,value){return `<label>${label}<output>${eqValue(key,value)}</output><input data-key="${key}" type="range" min="${min}" max="${max}" step="${key==="freq"?100:key==="gain"?10:50}" value="${value}"></label>`;}
  function eqValue(key,v){if(key==="freq")return v>=10000?`${(v/10000).toFixed(1)}k`:`${Math.round(v/10)}Hz`;if(key==="gain")return `${v>=0?"+":""}${(v/100).toFixed(1)}`;return (v/1000).toFixed(2);}
  function drawEq(ch){const canvas=$("eqGraph"),ctx=canvas.getContext("2d"),w=canvas.width,h=canvas.height;ctx.clearRect(0,0,w,h);ctx.strokeStyle="#26313b";ctx.lineWidth=1;for(let i=0;i<=8;i++){ctx.beginPath();ctx.moveTo(i*w/8,0);ctx.lineTo(i*w/8,h);ctx.stroke()}for(let i=0;i<=6;i++){ctx.beginPath();ctx.moveTo(0,i*h/6);ctx.lineTo(w,i*h/6);ctx.stroke()}ctx.strokeStyle=ch.peqOn?"#48c7ef":"#68737e";ctx.lineWidth=4;ctx.beginPath();for(let x=0;x<w;x++){const freq=20*Math.pow(1000,x/w);let gain=0;ch.eq.forEach(b=>{const center=b.freq/10,q=Math.max(.1,b.q/1000),distance=Math.log2(freq/center);gain+=(b.gain/100)*Math.exp(-distance*distance*q*q*2)});const y=h/2-clamp(gain,-18,18)/36*h;if(x===0)ctx.moveTo(x,y);else ctx.lineTo(x,y)}ctx.stroke();}
  function escapeHtml(value){return String(value).replace(/[&<>'"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"}[c]));}
  function closeModal(){mixModal.hidden=true;detailModal.hidden=true;}

  $("sofButton").addEventListener("click",()=>{state.sendsOnFader=!state.sendsOnFader;invoke("setSendsOnFader",state.sendsOnFader);render();});
  $("mixButton").addEventListener("click",openMix);$("detailButton").addEventListener("click",openDetail);
  $("setupButton").addEventListener("click",()=>native?invoke("openSetup"):alert("웹 디자인판은 실제 CL5 연결 설정을 제공하지 않습니다.\n실제 제어는 Android APK에서만 가능합니다."));
  document.querySelectorAll("[data-close]").forEach(b=>b.addEventListener("click",closeModal));
  [mixModal,detailModal].forEach(m=>m.addEventListener("click",e=>{if(e.target===m)closeModal()}));
  $("phantomButton").addEventListener("click",()=>invoke("requestPhantom",state.selectedChannel,!state.channels[state.selectedChannel].phantom));
  window.JerusalemMix={receive(json){try{state=JSON.parse(json);render()}catch(error){console.error(error)}},closeModal};
  if(native){try{state=JSON.parse(native.getState());}catch(error){console.error("Native state unavailable",error)}}
  render();
  if(location.protocol.startsWith("http")&&"serviceWorker" in navigator){addEventListener("load",async()=>{try{const r=await navigator.serviceWorker.register("sw.js");await r.update()}catch(error){console.warn("Offline cache unavailable",error)}})}
})();
