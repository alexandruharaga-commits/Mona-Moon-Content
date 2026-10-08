/* AURON v0.9.1 — read-only UX repair, no real-money operations. */
(function () {
  'use strict';
  const byId = (id) => document.getElementById(id);
  const SAFE_MODE = true;
  const HOME_DAYS = {'1D':'1','1W':'7','1M':'30','3M':'90','1Y':'365','ALL':'max'};
  const MARKET_BARS = {'1m':'1m','5m':'5m','15m':'15m','1H':'1H','4H':'4H','1D':'1D','1W':'1W'};
  let homePeriod='1D', marketPeriod='4H', homeSeq=0, marketSeq=0, activeCurrency='USD', fx=1;
  const marketArea = document.querySelector('#markets .chartBox');
  const rsiArea = document.querySelector('#markets .chartBox.rsi');
  const homeArea = byId('portfolioChart');
  const css=document.createElement('style');
  css.textContent='.shell{padding-top:max(32px,env(safe-area-inset-top,0px))!important;padding-bottom:calc(165px + env(safe-area-inset-bottom,0px))!important}.nav{bottom:calc(8px + env(safe-area-inset-bottom,0px))!important}.v091status{font-size:11px;color:#bcb6a6;margin:6px 4px 10px;line-height:1.5}.v091safe{margin:9px 0;padding:10px;border:1px solid #735525;border-radius:12px;color:#f0cd7c;background:#1b150b;font-size:12px}.v091interactive{cursor:pointer;border-radius:10px}.v091interactive:focus-visible{outline:2px solid #e3b858;outline-offset:2px}.v091currency{background:#111316;color:#f1cd83;cursor:pointer}.v091chart svg{width:100%;height:100%;display:block}.v091chart{height:100%;min-height:70px}.v091error{padding:15px;color:#bdb6a9;font-size:12px}.v091price{font-size:11px;color:#daba78}';
  document.head.appendChild(css);
  if (byId('profile')) {
    const badge=byId('profile').querySelector('article .pill');
    if(badge)badge.textContent='v0.9.1 · TEST SAFE';
  }
  // Remove static "LIVE" claims; an actual status is shown only after a successful response.
  const homeHeader=document.querySelector('#home .hero .eyebrow[style]');
  if(homeHeader)homeHeader.textContent='BTC MARKET CONTEXT · PUBLIC DATA';
  document.querySelectorAll('#home .pill.live, #markets .pill.live, #signal .pill.live').forEach((p)=>{p.classList.remove('live');p.textContent='STATUS UNVERIFIED';});
  const note=document.createElement('div'); note.className='v091safe';
  note.textContent='TEST SAFE · Read-only market monitoring. Approve, Reject, and Open OKX are locked in this test build.';
  const signalActions=document.querySelector('#signal .actions2');
  if(signalActions)signalActions.parentNode.insertBefore(note.cloneNode(true),signalActions);
  const tradeSection=document.querySelector('#trade article');
  if(tradeSection)tradeSection.insertAdjacentElement('afterend',note.cloneNode(true));
  // Safety is enforced at UI, state renderers, and action-entry points.
  const lockActions=()=>{
    ['approveBtn','rejectBtn','openOkx'].forEach((id)=>{const b=byId(id);if(b)b.disabled=true;});
    if(byId('signalActionHint'))byId('signalActionHint').textContent='TEST SAFE: trade decisions disabled; no orders can be submitted from this interface.';
  };
  if(typeof window.renderProposal==='function'){
    const original=window.renderProposal;
    window.renderProposal=function(p){original(p);lockActions();};
  }
  if(typeof window.renderTrade==='function'){
    const original=window.renderTrade;
    window.renderTrade=function(){original();lockActions();};
  }
  window.decide=function(){if(typeof window.toast==='function')window.toast('TEST SAFE: trading decisions are locked.');};
  document.addEventListener('click',(event)=>{
    if(event.target.closest('#approveBtn,#rejectBtn,#openOkx,#modalAction')){
      event.preventDefault();event.stopImmediatePropagation();
      if(typeof window.toast==='function')window.toast('TEST SAFE: real-money actions disabled.');
    }
  },true);
  lockActions();

  // Convert only publicly quoted USD numbers; never silently assume a foreign-exchange rate.
  const oldMoney=window.money;
  if(typeof oldMoney==='function'){
    window.money=function(value){
      const v=Number(value);
      if(!Number.isFinite(v))return activeCurrency==='EUR'?'€—':'$—';
      return new Intl.NumberFormat(undefined,{style:'currency',currency:activeCurrency,maximumFractionDigits:2}).format(v*(activeCurrency==='EUR'?fx:1));
    };
  }
  const oldRenderStatus=window.renderStatus;
  if(typeof oldRenderStatus==='function'){
    window.renderStatus=function(d){
      oldRenderStatus(d);lockActions();
      const p=document.querySelector('#home .insight .pill');
      if(p){p.textContent='BACKEND CONNECTED';p.classList.add('live');}
    };
  }
  const oldBackendStatus=window.backendStatus;
  if(typeof oldBackendStatus==='function'){
    window.backendStatus=async function(){
      await oldBackendStatus();
      const b=byId('okxBadge'),p=document.querySelector('#home .insight .pill');
      if(p && b && b.textContent==='OFFLINE'){p.textContent='BACKEND UNAVAILABLE';p.classList.remove('live');}
      lockActions();
    };
  }
  const curr=document.querySelector('#home .heroTop .pill');
  if(curr){
    const btn=document.createElement('button');
    btn.type='button';btn.className='pill v091currency';btn.id='currencySelector';btn.textContent='USD ▾';curr.replaceWith(btn);
    btn.addEventListener('click',async()=>{
      if(activeCurrency==='EUR'){
        activeCurrency='USD';fx=1;btn.textContent='USD ▾';redrawMoney();return;
      }
      btn.textContent='EUR …';btn.disabled=true;
      try{
        const r=await fetch('https://open.er-api.com/v6/latest/USD',{cache:'no-store'});
        if(!r.ok)throw new Error('FX unavailable');
        const data=await r.json(), rate=Number(data && data.rates && data.rates.EUR);
        if(data.result!=='success'||!(rate>0.5&&rate<1.5))throw new Error('Invalid FX rate');
        activeCurrency='EUR';fx=rate;btn.textContent='EUR ▾';redrawMoney();
      }catch(e){btn.textContent='USD ▾';activeCurrency='USD';fx=1;if(typeof window.toast==='function')window.toast('EUR exchange rate unavailable: showing USD.');}
      finally{btn.disabled=false;}
    });
  }
  function redrawMoney(){
    if(Array.isArray(window.state && window.state.market)){
      if(typeof window.renderCoins==='function')window.renderCoins(window.state.market);
      const btc=window.state.market.find(x=>x.id==='bitcoin');
      if(btc&&byId('marketPrice'))byId('marketPrice').textContent=window.money(btc.current_price);
    }
    if(window.state && window.state.status && typeof window.renderStatus==='function')window.renderStatus(window.state.status);
    if(typeof window.toast==='function')window.toast('Display currency: '+activeCurrency+' (converted from USD quotes).');
  }

  // Historical BTC values: every range requests a matching dataset, not the same 7-day sparkline.
  const homeLabel=document.createElement('div');homeLabel.className='v091status';homeLabel.id='homeChartStatus';
  if(homeArea)homeArea.insertAdjacentElement('afterend',homeLabel);
  function chartLine(points){
    const series=points.filter(p=>Array.isArray(p)&&Number.isFinite(Number(p[1]))).slice(-500);
    if(series.length<2)throw new Error('Insufficient price history');
    const sample=series.filter((_,i)=>i%Math.max(1,Math.floor(series.length/150))===0);
    if(sample[sample.length-1]!==series[series.length-1])sample.push(series[series.length-1]);
    const values=sample.map(p=>Number(p[1])),mn=Math.min(...values),mx=Math.max(...values),span=(mx-mn)||1;
    const coords=values.map((v,i)=>[(i/(values.length-1))*760,92-((v-mn)/span)*84]);
    const d=coords.map((p,i)=>(i?'L':'M')+p[0].toFixed(1)+' '+p[1].toFixed(1)).join(' ');
    return '<div class="v091chart"><svg viewBox="0 0 760 100" preserveAspectRatio="none" role="img" aria-label="BTC historical price line"><path d="'+d+'" stroke="#e7b85f" stroke-width="3" fill="none" vector-effect="non-scaling-stroke"/></svg></div>';
  }
  async function loadHome(){
    if(!homeArea)return;
    const seq=++homeSeq;homeLabel.textContent='Fetching BTC '+homePeriod+' history…';
    try{
      const url='https://api.coingecko.com/api/v3/coins/bitcoin/market_chart?vs_currency=usd&days='+HOME_DAYS[homePeriod];
      const r=await fetch(url,{cache:'no-store'});
      if(!r.ok)throw new Error('Price provider returned '+r.status);
      const data=await r.json();
      if(seq!==homeSeq)return;
      const prices=data.prices||[];
      homeArea.innerHTML=chartLine(prices);
      const last=prices[prices.length-1]||[];
      homeLabel.textContent='CoinGecko · '+homePeriod+' · Last historical point: '+(last[0]?new Date(Number(last[0])).toLocaleString():'unknown')+' · '+activeCurrency+' display';
    }catch(e){
      if(seq!==homeSeq)return;
      homeArea.innerHTML='<div class="v091error">Historical chart unavailable. No simulated prices shown.</div>';
      homeLabel.textContent='Historical BTC data unavailable: '+String(e.message||'network failure');
    }
  }
  const homeButtons=document.querySelectorAll('#home .timeframes button');
  homeButtons.forEach((button)=>{
    button.addEventListener('click',()=>{
      homePeriod=button.textContent.trim();
      homeButtons.forEach(b=>b.classList.toggle('active',b===button));
      loadHome();
    });
  });

  // OKX real public candles; never present a hard-coded SVG as live market data.
  if(marketArea)marketArea.innerHTML='<div id="marketCandles" class="v091error">Fetching real BTC/USDT candles…</div>';
  if(rsiArea)rsiArea.innerHTML='<div id="marketRsi" class="v091error">RSI(14) waiting for price history…</div>';
  const marketStatus=document.createElement('div');
  marketStatus.className='v091status';marketStatus.id='marketCandleStatus';
  if(marketArea)marketArea.insertAdjacentElement('beforebegin',marketStatus);
  function candlesSvg(candles){
    const recent=candles.slice(-75),width=760,height=250;
    const lows=recent.map(x=>x.low),highs=recent.map(x=>x.high);
    const low=Math.min(...lows),high=Math.max(...highs),spread=Math.max(0.00001,high-low),slot=width/recent.length;
    const y=p=>14+((high-p)/spread)*(height-30);
    let out='<svg viewBox="0 0 760 250" preserveAspectRatio="none" role="img" aria-label="OKX BTC/USDT candlestick chart">';
    for(let k=0;k<5;k++){const yy=15+k*55;out+='<path d="M0 '+yy+'H760" stroke="#292b2f" stroke-width="1"/>';}
    recent.forEach((x,i)=>{
      const cx=(i+.5)*slot,col=x.close>=x.open?'#18c989':'#f06d71';
      const yo=y(x.open),yc=y(x.close),top=Math.min(yo,yc),h=Math.max(1,Math.abs(yo-yc));
      out+='<path d="M'+cx.toFixed(2)+' '+y(x.high).toFixed(2)+'V'+y(x.low).toFixed(2)+'" stroke="'+col+'" stroke-width="1.6"/>';
      out+='<rect x="'+(cx-slot*.29).toFixed(2)+'" y="'+top.toFixed(2)+'" width="'+(slot*.58).toFixed(2)+'" height="'+h.toFixed(2)+'" fill="'+col+'"/>';
    });
    out+='</svg>';return out;
  }
  function computeRsi(candles){
    if(candles.length<16)return [];
    const closes=candles.map(x=>x.close),results=[];
    let gain=0,loss=0;
    for(let i=1;i<=14;i++){const d=closes[i]-closes[i-1];gain+=Math.max(d,0);loss+=Math.max(-d,0);}
    gain/=14;loss/=14;
    for(let i=15;i<closes.length;i++){
      const d=closes[i]-closes[i-1];
      gain=(gain*13+Math.max(d,0))/14;loss=(loss*13+Math.max(-d,0))/14;
      results.push(loss===0?(gain===0?50:100):(100-100/(1+gain/loss)));
    }
    return results.slice(-75);
  }
  function rsiSvg(candles){
    const values=computeRsi(candles);
    if(values.length<2)return '<div class="v091error">Not enough data for RSI(14).</div>';
    const d=values.map((v,i)=>(i?'L':'M')+(i/(values.length-1)*760).toFixed(2)+' '+(68-v*.58).toFixed(2)).join(' ');
    return '<svg viewBox="0 0 760 74" preserveAspectRatio="none" role="img" aria-label="RSI 14 derived from real candles"><path d="M0 27H760M0 50H760" stroke="#353638" stroke-dasharray="5 5"/><path d="'+d+'" stroke="#e8b85e" stroke-width="3" fill="none"/></svg>';
  }
  async function loadCandles(){
    if(!marketArea)return;
    const seq=++marketSeq;
    marketStatus.textContent='Loading OKX '+marketPeriod+' candles…';
    const target=byId('marketCandles'),rsi=byId('marketRsi');
    try{
      const url='https://www.okx.com/api/v5/market/candles?instId=BTC-USDT&bar='+encodeURIComponent(MARKET_BARS[marketPeriod])+'&limit=100';
      const r=await fetch(url,{cache:'no-store'});
      if(!r.ok)throw new Error('Market feed returned '+r.status);
      const data=await r.json();
      if(data.code!=='0'||!Array.isArray(data.data))throw new Error('Invalid exchange response');
      const candles=data.data.map(x=>({at:Number(x[0]),open:Number(x[1]),high:Number(x[2]),low:Number(x[3]),close:Number(x[4]),confirmed:String(x[8])==='1'}))
        .filter(x=>[x.at,x.open,x.high,x.low,x.close].every(Number.isFinite)&&x.low>0).sort((a,b)=>a.at-b.at);
      if(candles.length<20)throw new Error('Insufficient valid candles');
      if(seq!==marketSeq)return;
      target.className='v091chart';target.innerHTML=candlesSvg(candles);
      rsi.className='v091chart';rsi.innerHTML=rsiSvg(candles);
      const last=candles[candles.length-1];
      marketStatus.textContent='OKX spot BTC/USDT · '+marketPeriod+' · Latest candle '+new Date(last.at).toLocaleString()+' · '+(last.confirmed?'closed':'forming')+' · Read-only';
    }catch(e){
      if(seq!==marketSeq)return;
      target.className='v091error';target.textContent='Live candles unavailable. No simulated chart shown.';
      rsi.className='v091error';rsi.textContent='RSI requires valid market candles.';
      marketStatus.textContent='OKX market feed unavailable: '+String(e.message||'network failure');
    }
  }
  const marketButtons=document.querySelectorAll('#markets .timeframes button');
  marketButtons.forEach(button=>button.addEventListener('click',()=>{
    marketPeriod=button.textContent.trim();
    marketButtons.forEach(b=>b.classList.toggle('active',b===button));
    loadCandles();
  }));
  const refresh=byId('marketRefresh');if(refresh)refresh.addEventListener('click',loadCandles);

  // Plan, Help and About now open useful informational screens without inventing billing/support.
  const items=document.querySelectorAll('#profile .setting');
  const info=[
    ['AURON Private Beta','Plan: Private Beta. This is a non-commercial test. No paid plan or subscription checkout is connected. Market data and risk safeguards are being validated.'],
    ['Help & Support','Use Markets to inspect BTC/USDT candles. If data is unavailable, verify your internet connection and retry Refresh. The backend and OKX account status are shown under Settings. A support contact has not been configured yet.'],
    ['About AURON','AURON v0.9.1 · TEST SAFE. Decision-support prototype with CoinGecko prices, OKX public candles, and optional read-only backend status. Trading approval and OKX order handoff are disabled in this test build. Crypto trading involves substantial risk.']
  ];
  items.forEach((item,i)=>{
    if(i>=info.length)return;
    item.classList.add('v091interactive');item.tabIndex=0;item.setAttribute('role','button');
    const open=()=>{
      byId('modalTitle').textContent=info[i][0];
      byId('modalText').textContent=info[i][1];
      byId('modalAction').style.display='none';
      byId('modal').classList.add('show');
    };
    item.addEventListener('click',open);
    item.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();open();}});
  });
  byId('modalClose').onclick=()=>{byId('modal').classList.remove('show');byId('modalAction').style.display='';};
  // The original trade modal uses this button for exchange handoff; safe mode never does.
  byId('modalAction').onclick=()=>{byId('modal').classList.remove('show');byId('modalAction').style.display='';};

  function activeScreen(){return document.querySelector('.screen.active')?.id;}
  function maybeRefresh(){const screen=activeScreen();if(screen==='home')loadHome();if(screen==='markets')loadCandles();}
  document.addEventListener('click',(e)=>{
    const go=e.target.closest('[data-go]');
    if(go && (go.dataset.go==='home'||go.dataset.go==='markets'))setTimeout(maybeRefresh,0);
    if(e.target.id==='loginBtn'||e.target.id==='demoBtn')setTimeout(maybeRefresh,150);
  });
  if(activeScreen()==='home'||activeScreen()==='markets')setTimeout(maybeRefresh,20);
  setInterval(()=>{if(!document.hidden)maybeRefresh();},60000);
})();