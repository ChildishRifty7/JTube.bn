package ui.custom;

import javax.microedition.lcdui.CustomItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;

public abstract class CustomButtonItem extends CustomItem implements UIConstants {
	
	//private ItemCommandListener l;
	//private boolean pressed;

	protected CustomButtonItem(ItemCommandListener l) {
		super(null);
		setLayout(Item.LAYOUT_EXPAND);
		//this.l = l;
	}
	
	/*
	public void pointerPressed(int x, int y) {
		pressed = true;
	}
	
	public void pointerDragged(int x, int y) {
		pressed = false;
	}
	
	public void pointerReleased(int x, int y) {
		if(pressed) {
			callCommandOK();
			pressed = false;
		}
	}

	private void callCommandOK() {
		if(l != null) {
			l.commandAction(null, this);
		}
	}
	*/

}
