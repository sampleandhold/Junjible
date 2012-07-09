// ======================================================= 
//   __|     _ |     |           | 
//  (_ |  -_)  |  _` |  _ \  _ \  _| 
// \___|\___| _|\__,_|\___/\___/\__| 
// ----------------------------------------------------- 
//               - virtual 3D gallery - 
// script by Gerard Ferrandez - Ge-1-doot - October 2004 
// http://www.dhteumeuleu.com 
// DOM crossbrowser version: GF Feb 2005 
// Updated Jan 2010: complete rewrite + CSS walls 
// ======================================================= 
 
var v3D = function () { 
	/* ---- private vars ---- */ 
	var scr, 
	    nx, 
	    ny, 
	    nw, 
	    nh, 
	    xm = 0, 
		ym = 0, 
		y0 = 0, 
		camera = { 
			x : 0, 
			y : 11100, 
			z : 10000 
		}, 
		N = 0, 
	    oT = [], 
		pT = [], 
		Zeye = -100, 
		hm = 20, 
		cb = true, 
		mv = false; 
	/* ====== create HTML ====== */ 
	var createHTML = function (tag, container, clas, src, html) { 
		var obj = document.createElement(tag); 
		if (clas) obj.className = clas; 
		if (src) obj.src = src; 
		if (html) obj.innerHTML = html; 
		container.appendChild(obj); 
		return obj; 
	} 
	/* ====== DOM events ====== */ 
	var addEvent = function (o, e, f) { 
		if (window.addEventListener) o.addEventListener(e, f, false); 
		else if (window.attachEvent) r = o.attachEvent('on' + e, f); 
	} 
	/* ====== screen dimensions ====== */ 
	var resize = function () { 
		nw = scr.offsetWidth * .5; 
		nh = scr.offsetHeight * .5; 
		var o = scr; 
		for (nx = 0, ny = 0; o != null; o = o.offsetParent) { 
			nx += o.offsetLeft; 
			ny += o.offsetTop; 
		} 
	} 
	/* ====== zOOm ====== */ 
	var zOOm = function (z) { 
		var z0 = Zeye - z; 
		if (z0 > 0 ) z0 = -.0001; 
		z0 = Zeye / z0; 
		if(z0 > 10) z0 = 10; 
		cb = (z > Zeye * .9); 
		return z0; 
	} 
	/* ====== Images constructor ====== */ 
	var Tab = function (img) { 
		/* ---- get params from HTML properties ---- */ 
		var coord = img.alt.split(","); 
		img.alt = ""; 
		this.X = coord[0] * 100; 
		this.Y = coord[1] * 100; 
		this.Z = coord[2] * 100; 
		this.W = coord[3] * 100; 
		this.H = coord[4] * 100; 
		/* ---- create frame ---- */ 
		this.pol = createHTML("div", scr, "trans"); 
		this.pol.style.zIndex = 1000 - Math.round(this.Z); 
		this.img = img; 
		this.img.style.zIndex = 1001 - Math.round(this.Z); 
		/* ---- click on images ---- */ 
		this.img.obj = this; 
		if (!this.img.parentNode.href) { 
			this.img.style.cursor = "crosshair"; 
			this.img.onclick = function () { 
				hm = -80 - this.obj.Z; 
				return false; 
			} 
		} 
		/* ---- create text ---- */ 
		this.txt = createHTML("div", scr, "txt"); 
		this.txt.style.zIndex = 1010 - Math.round(this.Z); 
		var txt = createHTML("div", this.txt, "plo", "","" + img.title + ""); 
		this.txt.txt = txt; 
		this.L = img.title.length * 2; 
		img.title = ""; 
		/* ---- CSS3 text Transform ---- */ 
		if ('MozTransform' in document.body.style) { 
			this.txt.css3Transform = function (x, y, s) { 
				this.style.MozTransform = "translate(" + x + "px," + y + "px) scale(" + s + ")"; 
			} 
		} else if ('webkitTransform' in document.body.style) { 
			this.txt.css3Transform = function (x, y, s) { 
				this.style.webkitTransform = "translate(" + x + "px," + y + "px) scale(" + s + ")"; 
			} 
		} else if (scr.style.zoom) { 
			this.txt.css3Transform = function (x, y, s) { 
				this.style.left = x + 'px'; 
				this.style.top = y + 'px'; 
				this.txt.style.zoom = s; 
			} 
		} else { 
			this.txt.css3Transform = function (x, y, s) { 
				this.style.left = x + 'px'; 
				this.style.top = y + 'px'; 
				this.txt.style.fontSize = Math.round(s * 10) + "px"; 
			} 
		} 
	} 
	Tab.prototype.anim = function () { 
		var z0 = zOOm(camera.y + this.Z); 
		if (cb) { 
			var x = (camera.x + this.X) * z0; 
			var y = (camera.z + this.Y) * z0; 
			/* ---- transparent frame ---- */ 
			var css = this.pol.style; 
			css.visibility = "visible"; 
			css.left   = Math.round(nw + x) + "px"; 
			css.top    = Math.round(nh + y) + "px"; 
			css.width  = Math.round(this.W * z0) + "px"; 
			css.height = Math.round(this.H * z0) + "px"; 
			/* ---- image ---- */ 
			css = this.img.style; 
			css.visibility = "visible"; 
			css.left   = Math.round(nw + x + (this.W * z0 * .1)) + "px"; 
			css.top    = Math.round(nh + y + (this.H * z0 * .1)) + "px"; 
			css.width  = Math.round(this.W * z0 * .8) + "px"; 
			css.height = Math.round(this.H * z0 * .8) + "px"; 
			/* ---- title ---- */ 
			if (z0 > .3 ) { 
				this.txt.style.visibility = "visible"; 
				this.txt.css3Transform( 
					Math.round(nw - (this.L * z0) + (this.W * z0 * 0.5) + (x * 1.3)), 
					Math.round(nh + (this.H * z0 * 0.2) + (y * 1.3)), 
					z0 * 1 
				); 
			} else this.txt.style.visibility = "hidden"; 
		} else { 
			/* ---- behind camera ---- */ 
			this.pol.style.visibility = "hidden"; 
			this.img.style.visibility = "hidden"; 
			this.txt.style.visibility = "hidden"; 
		} 
	} 
	/* ====== Walls constructor ====== */ 
	var Wall = function (X, Y, Z) { 
		this.X = X; 
		this.Y = Y; 
		this.Z = Z; 
		this.o1 = createHTML("div", scr, "wall"); 
		this.o2 = createHTML("div", scr, "wall"); 
		this.o3 = createHTML("div", scr, "wall"); 
	} 
	Wall.prototype.anim = function () { 
		var x = []; 
		var y = []; 
		/* ---- 3D ---- */ 
		for (var i = 0; i < 4; i++) { 
			var z0 = zOOm(camera.y + this.Z[i] * 100); 
			x[i] = Math.round(nw + (camera.x + this.X[i] * 100) * z0); 
			y[i] = Math.round(nh + (camera.z + this.Y[i] * 100) * z0); 
		} 
		/* ---- clipping ---- */ 
		if(x[1] < 0 || x[0] > nw * 2) { 
			x[0] = 0; 
			y[0] = 0; 
			x[1] = 0; 
			y[1] = 0; 
			x[2] = 0; 
			y[2] = 0; 
			x[3] = 0; 
			y[3] = 0; 
		} 
		if(x[0] < 0) { 
			var k = (y[1] - y[0]) / (x[1] - x[0]) * x[1]; 
			x[0] = 0; 
			y[0] = Math.round(y[1] - k); 
			k = (y[3] - y[2]) / (x[2] - x[3]) * x[2]; 
			x[3] = 0; 
			y[3] = Math.round(y[2] + k); 
		} 
		if(x[1] > nw * 2) { 
			var k = (y[0] - y[1]) / (x[1] - x[0]) * ((nw * 2) - x[0]); 
			x[1] = nw * 2; 
			y[1] = Math.round(y[0] - k); 
			k = (y[2] - y[3]) / (x[2] - x[3]) * ((nw * 2) - x[3]); 
			x[2] = nw * 2; 
			y[2] = Math.round(y[3] + k); 
		} 
		/* ---- top ---- */ 
		var css = this.o1.style; 
		if( y[1] >= y[0]) { 
			css.borderColor = "transparent transparent transparent transparent"; 
			css.borderWidth = Math.round(y[1] - y[0]) + "px 0  0 " + Math.round(x[1] - x[0]) + "px"; 
			css.left        = Math.round(x[0]) + "px"; 
			css.top         = Math.round(y[0]) + "px"; 
		} else { 
			css.borderColor = "transparent transparent transparent transparent"; 
			css.borderWidth = "0 0 " + Math.round(y[0] - y[1]) + "px " + Math.round(x[1] - x[0]) + "px"; 
			css.left        = Math.round(x[0]) + "px"; 
			css.top         = Math.round(y[1]) + "px"; 
		} 
		/* ---- bottom ---- */ 
		css = this.o3.style; 
		if(y[3] >= y[2]){ 
			css.borderColor = "transparent transparent transparent transparent"; 
			css.borderWidth = "0 0 " + Math.round(y[3] - y[2]) + "px " + Math.round(x[1] - x[0]) + "px"; 
			css.left        = Math.round(x[0]) + "px"; 
			css.top         = Math.round(y[2]) + "px"; 
		} else { 
			css.borderColor = "transparent transparent transparent transparent"; 
			css.borderWidth = Math.round(y[2] - y[3]) + "px 0  0 " + Math.round(x[1] - x[0]) + "px"; 
			css.left        = Math.round(x[0]) + "px"; 
			css.top         = Math.round(y[3]) + "px"; 
		} 
		/* ---- middle ---- */ 
		css = this.o2.style; 
		css.left            = Math.round(x[0]) + "px"; 
		css.top             = Math.round(Math.max(y[0], y[1])) + "px"; 
		css.width           = Math.round(x[1] - x[0]) + "px"; 
		css.height          = Math.round(Math.min(y[2], y[3]) - Math.max(y[0], y[1])) + "px"; 
		css.background      = "transparent"; 
	} 
 
	/* ====== init script ====== */ 
	var init = function () { 
		scr = document.getElementById("screen"); 
		resize(); 
		addEvent(window, 'resize', resize); 
		/* ---- mouse move event ---- */ 
		addEvent(document, 'mousemove', function (e) { 
			if (window.event) e = window.event; 
			xm = e.clientX; 
			y0 = e.clientY; 
			xm =  (nw - xm) * (250 / nw); 
			ym =  (nh - y0) * (110 / nh); 
		}); 
		/* ---- mouse wheel - camera z position ---- */ 
		if (window.addEventListener) scr.addEventListener('DOMMouseScroll', function(e) { 
			if (e.preventDefault) e.preventDefault(); 
			hm -= (e.detail * 10); 
			return false; 
		}, false); 
		scr.onmousewheel = function () { 
			hm += (event.wheelDelta * .25); 
			return false; 
		} 
		/* ---- create walls ---- */ 
		oT.push(new Wall( 
			[-2.52,-2.52,-2.52,-2.52], 
			[-1,-1,1,1], 
			[-1,1,1,-1] 
		)); 
		oT.push(new Wall( 
			[-2.5,2.5,2.5,-2.5], 
			[-1,-1,1,1], 
			[1,1,1,1] 
		)); 
		oT.push(new Wall( 
			[2.52,2.52,2.52,2.52], 
			[-1,-1,1,1], 
			[1,-.5,-.5,1] 
		)); 
		/* ---- create images ---- */ 
		var i = 0, o; 
		var img = scr.getElementsByTagName("img"); 
		while (o = img[i++]) pT.push(new Tab(o)); 
		/* ---- start main loop ---- */ 
		run();
	} 
	/* ====== main loop ====== */ 
	var run = function () { 
		/* ---- easing camera ---- */ 
		camera.x += (xm - camera.x) / 10; 
		camera.y += (hm - camera.y) / 10; 
		camera.z += (ym - camera.z) / 10; 
		/* ---- walls ---- */ 
		var i = 0, o; 
		while ( o = oT[i++] ) o.anim(); 
		/* ---- images ---- */ 
		var i = 0, o; 
		while ( o = pT[i++] ) o.anim(); 
    	setTimeout(run, 16); 
	} 
	return { 
		/* ====== initialize script ====== */ 
		init : function () { 
			addEvent(window, 'load', function () { 
				init(); 
			}); 
		} 
	} 
}(); 
 
/* ====== start ====== */ 
v3D.init(); 
 