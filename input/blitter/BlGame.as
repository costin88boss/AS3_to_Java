﻿package blitter
{
	import flash.display.Bitmap;
	import flash.display.BitmapData;
	import flash.display.DisplayObject;
	import flash.display.MovieClip;
	import flash.display.PixelSnapping;
	import flash.display.Sprite;
	import flash.display.StageScaleMode;
	import flash.events.Event;
	import flash.utils.setInterval;
	
	import states.PlayState;
	
	import ui.OverlayContainer;
	
	public class BlGame extends MovieClip
	{
		protected var screen:BitmapData
		protected var screenContainer:Bitmap
		public var overlayContainer:OverlayContainer = new OverlayContainer();
		protected var _state:BlState;
		
		
		protected var bw:int = 0;
		protected var bh:int = 0;
		
		public function BlGame(width:int, height:int, scale:Number)
		{
			
			screen = new BitmapData(width, height - 10, false, 0x0);
			
			screenContainer = new Bitmap(screen, PixelSnapping.ALWAYS, false);
			Bl.screenContainer = screenContainer;
			
			screenContainer.width = width * scale;
			screenContainer.height = height * scale-10;
			
			bw = width;
			bh = height;

			addChild(screenContainer);
			
			overlayContainer.addEventListener(Event.ADDED,handleNewOverlay,false,0,true);
			overlayContainer.addEventListener(Event.REMOVED,handleNewOverlay,false,0,true);
			Bl.overlayContainer = overlayContainer;
			
			addEventListener(Event.ADDED_TO_STAGE,handleAttach);		
			
			//addChild(new Stats())
		}
		
		public function clearOverlayContainer():void
		{
			while(overlayContainer.numChildren > 0){
				overlayContainer.removeChildAt(0);
			}
			
		}
		
		protected function handleNewOverlay(event:Event):void{
			// to make sure the height and width of the container is accurate
			addEventListener(Event.EXIT_FRAME,handleExitFrame,false,0,true);
		}
		
		protected function handleExitFrame(event:Event):void
		{
			removeEventListener(Event.EXIT_FRAME,handleExitFrame);
			alignOverlay();
		}
		
		protected function alignOverlay():void{
			var align:String = state != null? state.align: BlState.STATE_ALIGN_CENTER;
			switch(align)
			{
				case BlState.STATE_ALIGN_LEFT:
				{
					overlayContainer.x = 0;	
					if(stage.stageWidth > Config.maxwidth) overlayContainer.x = (stage.stageWidth-Config.maxwidth)/2>>0;
					break;
				}
				case BlState.STATE_ALIGN_CENTER:
				{
					overlayContainer.x = (stage.stageWidth-overlayContainer.width)/2>>0;	
					break;
				}
				case BlState.STATE_ALIGN_RIGHT:
				{
					overlayContainer.x = (stage.stageWidth-overlayContainer.width);
					break;
				}
			}
			Bl.stage.addChild(overlayContainer);			
		}
		
		protected function handleResize(event:Event = null):void{
			alignOverlay();		
			if(state != null){
				state.resize();
				switch(state.align)
				{
					case BlState.STATE_ALIGN_LEFT:
					{
						screenContainer.x = 0;
						if(stage.stageWidth > Config.maxwidth) screenContainer.x = (stage.stageWidth-Config.maxwidth)/2>>0;
						break;
					}
					case BlState.STATE_ALIGN_CENTER:
					{
						screenContainer.x = (stage.stageWidth-bw)/2>>0;
						break;
					}
					case BlState.STATE_ALIGN_RIGHT:
					{
						screenContainer.x = (stage.stageWidth-bw);
						break;
					}
				}
			}
			
			
		}
		
		private function handleAttach(e:Event):void{
			Bl.init(stage, bw, bh);
			stage.frameRate = Config.maxFrameRate
			addEventListener(Event.ENTER_FRAME, handleEnterFrame)
			stage.addEventListener(Event.RESIZE,handleResize);		
			stage.addChild(overlayContainer);
		}
		
		
		public function get state():BlState{
			return _state
		}

		public function set state(s:BlState):void{
			if (_state) _state.killed();
			_state = s;
			handleResize();
			//Bl.update();
		}
		
		protected function handleEnterFrame(e:Event = null):void{
			if( state != null && !state.stopRendering){ //state = game
				//Bl.update();
				if((new Date().time-Bl.time)/Config.physics_ms_per_tick > 15){
					Bl.time = new Date().time - Config.physics_ms_per_tick*15 // update time
				}
				
				
				for(;Bl.time<new Date().time;Bl.time+=Config.physics_ms_per_tick){
					state.tick(); // update physics based off ticks
				}
				state.enterFrame()
				Bl.exitFrame();
				state.exitFrame()
				screen.lock();				
				state.draw(screen,0,0);
				screen.unlock()
			}
		}
	}
}