package betterquesting.client.gui2.editors;

import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.client.gui.misc.IVolatileScreen;
import betterquesting.api.enums.EnumPacketAction;
import betterquesting.api.enums.EnumSaveType;
import betterquesting.api.network.QuestingPacket;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.controls.PanelButtonStorage;
import betterquesting.api2.client.gui.controls.PanelTextField;
import betterquesting.api2.client.gui.controls.filters.FieldFilterString;
import betterquesting.api2.client.gui.events.IPEventListener;
import betterquesting.api2.client.gui.events.PEventBroadcaster;
import betterquesting.api2.client.gui.events.PanelEvent;
import betterquesting.api2.client.gui.events.types.PEventButton;
import betterquesting.api2.client.gui.misc.*;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelLine;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetIcon;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui.editors.GuiQuestLineDesigner;
import betterquesting.network.PacketSender;
import betterquesting.network.PacketTypeNative;
import betterquesting.questing.QuestLineDatabase;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector4f;

public class GuiQuestLinesEditor extends GuiScreenCanvas implements IPEventListener, IVolatileScreen, INeedsRefresh
{
    private CanvasScrolling lineList;
    private PanelTextField<String> tfName;
    private PanelTextField<String> tfDesc;
    
    private PanelButton btnDesign;
    
    private IQuestLine selected;
    private int selID = -1;
    
    public GuiQuestLinesEditor(GuiScreen parent)
    {
        super(parent);
    }
    
    @Override
    public void refreshGui()
    {
        if(selID >= 0)
        {
            selected = QuestLineDatabase.INSTANCE.getValue(selID);
            
            if(selected == null)
            {
                selID = -1;
                btnDesign.setActive(false);
                tfName.setText("");
                tfDesc.setText("");
            } else
            {
                if(!tfName.isFocused()) tfName.setText(selected.getUnlocalisedName());
                if(!tfDesc.isFocused()) tfDesc.setText(selected.getUnlocalisedDescription());
            }
        }
        
        reloadList();
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
        
        PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
        Keyboard.enableRepeatEvents(true);
        
        // Background panel
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        
        PanelTextBox panTxt = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 0, -32), 0), QuestTranslation.translate("betterquesting.title.edit_line1")).setAlignment(1);
        panTxt.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(panTxt);
        
        cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 200, 16, 0), 0, QuestTranslation.translate("gui.back")));
        
        // Left side
        
        lineList = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(16, 32, 16, 48), 0));
        cvBackground.addPanel(lineList);
        
        PanelVScrollBar scList = new PanelVScrollBar(new GuiTransform(new Vector4f(0.5F, 0F, 0.5F, 1F), new GuiPadding(-16, 32, 8, 48), 0));
        cvBackground.addPanel(scList);
        lineList.setScrollDriverY(scList);
        
        PanelButton btnAdd = new PanelButton(new GuiTransform(new Vector4f(0F, 1F, 0.25F, 1F), new GuiPadding(16, -40, 0, 24), 0), 1, QuestTranslation.translate("betterquesting.btn.new"));
        cvBackground.addPanel(btnAdd);
        
        PanelButton btnImport = new PanelButton(new GuiTransform(new Vector4f(0.25F, 1F, 0.5F, 1F), new GuiPadding(0, -40, 16, 24), 0), 2, QuestTranslation.translate("betterquesting.btn.import"));
        cvBackground.addPanel(btnImport);
        
        // Right side
    
        CanvasEmpty cvRight = new CanvasEmpty(new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(8, 32, 16, 24), 0));
        cvBackground.addPanel(cvRight);
        
        PanelTextBox txtName = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 4, 0, -16), 0), QuestTranslation.translate("betterquesting.gui.name"));
        txtName.setColor(PresetColor.TEXT_MAIN.getColor());
        cvRight.addPanel(txtName);
    
        tfName = new PanelTextField<>(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 0, -32), 0), "", FieldFilterString.INSTANCE);
        cvRight.addPanel(tfName);
        
        PanelTextBox txtDesc = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 36, 0, -48), 0), QuestTranslation.translate("betterquesting.gui.description"));
        txtDesc.setColor(PresetColor.TEXT_MAIN.getColor());
        cvRight.addPanel(txtDesc);
    
        tfDesc = new PanelTextField<>(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 48, 16, -64), 0), "", FieldFilterString.INSTANCE);
        tfDesc.setMaxLength(Integer.MAX_VALUE);
        tfDesc.setCallback(value -> {
        
        });
        cvRight.addPanel(tfDesc);
        
        PanelButton btnManage = new PanelButton(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 80, 0, -96), 0), 3, QuestTranslation.translate("betterquesting.btn.add_remove_quests"));
        cvRight.addPanel(btnManage);
        
        btnDesign = new PanelButton(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 96, 0, -112), 0), 4, QuestTranslation.translate("betterquesting.btn.designer"));
        btnDesign.setActive(selected != null);
        cvRight.addPanel(btnDesign);
        
        PanelButton btnTextEditor = new PanelButton(new GuiTransform(GuiAlign.TOP_RIGHT, new GuiPadding(-16, 48, 0, -64), 0), 8, "Aa");
        cvRight.addPanel(btnTextEditor);
        
        // Dividers
        
        IGuiRect ls0 = new GuiTransform(GuiAlign.TOP_CENTER, 0, 32, 0, 0, 0);
		ls0.setParent(cvBackground.getTransform());
		IGuiRect le0 = new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, -24, 0, 0, 0);
		le0.setParent(cvBackground.getTransform());
		PanelLine paLine0 = new PanelLine(ls0, le0, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), 1);
		cvBackground.addPanel(paLine0);
		
		if(selID >= 0)
        {
            selected = QuestLineDatabase.INSTANCE.getValue(selID);
            
            if(selected == null)
            {
                selID = -1;
                btnDesign.setActive(false);
                tfName.setText("");
                tfDesc.setText("");
            } else
            {
                if(!tfName.isFocused()) tfName.setText(selected.getUnlocalisedName());
                if(!tfDesc.isFocused()) tfDesc.setText(selected.getUnlocalisedDescription());
            }
        }
		
		reloadList();
    }
    
    @Override
    public boolean onMouseClick(int mx, int my, int click)
    {
        if(selected != null)
        {
            boolean changed = false;
    
            if(!tfName.getValue().equals(selected.getUnlocalisedName()))
            {
                selected.getProperties().setProperty(NativeProps.NAME, tfName.getValue());
                changed = true;
            }
            
            if(!tfDesc.getValue().equals(selected.getUnlocalisedDescription()))
            {
                selected.getProperties().setProperty(NativeProps.DESC, tfDesc.getValue());
                changed = true;
            }
            
            if(changed)
            {
                SendChanges(EnumPacketAction.EDIT, new DBEntry<>(selID, selected));
            }
        }
        
        return super.onMouseClick(mx, my, click);
    }
    
    @Override
    public void onPanelEvent(PanelEvent event)
    {
        if(event instanceof PEventButton)
        {
            onButtonPress((PEventButton)event);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void onButtonPress(PEventButton event)
    {
        IPanelButton btn = event.getButton();
    
        if(btn.getButtonID() == 0) // Exit
        {
            mc.displayGuiScreen(this.parent);
        } else if(btn.getButtonID() == 1) // New Quest Line
        {
			SendChanges(EnumPacketAction.ADD, null);
        } else if(btn.getButtonID() == 2) // Import
        {
            mc.displayGuiScreen(new GuiImporters(this));
        } else if(btn.getButtonID() == 3) // Add/Remove Quests
        {
            mc.displayGuiScreen(new GuiQuestLineAddRemove(this, selected));
        } else if(btn.getButtonID() == 4 && selected != null) // Designer
        {
			mc.displayGuiScreen(new GuiQuestLineDesigner(this, selected));
			//mc.displayGuiScreen(new GuiDesigner(this, selected));
        } else if(btn.getButtonID() == 5 && btn instanceof PanelButtonStorage) // Select Quest
        {
            DBEntry<IQuestLine> entry = ((PanelButtonStorage<DBEntry<IQuestLine>>)btn).getStoredValue();
            selected = entry.getValue();
            selID = entry.getID();
            tfName.setText(selected.getUnlocalisedName());
            tfDesc.setText(selected.getUnlocalisedDescription());
            btnDesign.setActive(true);
            
            reloadList();
        } else if(btn.getButtonID() == 6 && btn instanceof PanelButtonStorage) // Delete Quest
        {
            DBEntry<IQuestLine> entry = ((PanelButtonStorage<DBEntry<IQuestLine>>)btn).getStoredValue();
            SendChanges(EnumPacketAction.REMOVE, entry);
        } else if(btn.getButtonID() == 7 && btn instanceof PanelButtonStorage) // Move Up
        {
            DBEntry<IQuestLine> entry = ((PanelButtonStorage<DBEntry<IQuestLine>>)btn).getStoredValue();
            int order = QuestLineDatabase.INSTANCE.getOrderIndex(entry.getID());
            
            if(order > 0)
            {
                SendChanges(EnumPacketAction.EDIT, entry, order - 1);
            }
        } else if(btn.getButtonID() == 8) // Big Description Editor
        {
            mc.displayGuiScreen(new GuiTextEditor(this, tfDesc.getRawText(), value -> {
                if(selected != null)
                {
                    tfDesc.setText(value);
                    selected.getProperties().setProperty(NativeProps.DESC, value);
                    SendChanges(EnumPacketAction.EDIT, new DBEntry<>(selID, selected));
                }
            }));
        }
    }
    
    private void reloadList()
    {
        lineList.resetCanvas();
        
        int w = lineList.getTransform().getWidth();
        int i = 0;
        
        for(DBEntry<IQuestLine> entry : QuestLineDatabase.INSTANCE.getSortedEntries())
        {
            IQuestLine ql = entry.getValue();
            PanelButtonStorage<DBEntry<IQuestLine>> tmp = new PanelButtonStorage<>(new GuiRectangle(0, i * 16, w - 32, 16, 0), 5, QuestTranslation.translate(ql.getUnlocalisedName()), entry);
            tmp.setActive(entry.getID() != selID);
            lineList.addPanel(tmp);
            lineList.addPanel(new PanelButtonStorage<>(new GuiRectangle(w - 32, i * 16, 16, 16, 0), 6, "", entry).setIcon(PresetIcon.ICON_TRASH.getTexture()));
            lineList.addPanel(new PanelButtonStorage<>(new GuiRectangle(w - 16, i * 16, 16, 16, 0), 7, "", entry).setIcon(PresetIcon.ICON_UP.getTexture()));
            i++;
        }
    }

	private void SendChanges(EnumPacketAction action, DBEntry<IQuestLine> entry)
	{
		SendChanges(action, entry, entry == null? -1 : QuestLineDatabase.INSTANCE.getOrderIndex(entry.getID()));
	}
	
	private void SendChanges(EnumPacketAction action, DBEntry<IQuestLine> entry, int order)
	{
		if(action == null)
		{
			return;
		}
		
		NBTTagCompound tags = new NBTTagCompound();
		
		if(action == EnumPacketAction.EDIT && entry != null)
		{
			NBTTagCompound base = new NBTTagCompound();
			base.setTag("line", entry.getValue().writeToNBT(new NBTTagCompound(), EnumSaveType.CONFIG));
			tags.setTag("data", base);
		}
		
		tags.setInteger("lineID", entry == null? -1 : entry.getID());
		tags.setInteger("order", order);
		tags.setInteger("action", action.ordinal());
		
		PacketSender.INSTANCE.sendToServer(new QuestingPacket(PacketTypeNative.LINE_EDIT.GetLocation(), tags));
	}
}