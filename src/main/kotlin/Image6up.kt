/*
    Image6up

    画像を6枚分貼り付けて印刷するために整形するプログラム

    Copyright © 2021 gikoha
 */

import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.Printable.NO_SUCH_PAGE
import java.awt.print.Printable.PAGE_EXISTS
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.util.*
import javax.print.attribute.HashPrintJobAttributeSet
import javax.print.attribute.standard.Copies
import javax.print.attribute.standard.MediaPrintableArea
import javax.print.attribute.standard.MediaSizeName
import javax.swing.*


var imagesPanel: Vector<JPanel> = Vector<JPanel>()
var images: Vector<Image> = Vector<Image>()

class customListCellRenderer : DefaultListCellRenderer()
{
    public override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component
    {
        val component = value as Component
        component.background = if (isSelected)
        {
            Color.black
        } else
        {
            Color.white
        }
        component.foreground = if (isSelected)
        {
            Color.white
        } else
        {
            Color.black
        }
        return component
    }
}

class printOnePage : Printable
{
    override fun print(graphics: Graphics?, pageFormat: PageFormat?, pageIndex: Int): Int
    {
        if (pageIndex != 0) return NO_SUCH_PAGE;
        var g2 = graphics as Graphics2D
        //        g2.setFont(Font(DIALOG,PLAIN,12))
        //        g2.setStroke(BasicStroke(0.5f ))
        //        g2.drawString("ogehage",0,0)
        var j = 0
        for (i in images)
        {
            var xx = 250 * (j / 3)
            var yy = 250 * (j % 3)
            g2.drawImage(i.getScaledInstance(240, 240, Image.SCALE_SMOOTH), xx, yy, null)

            j++
        }
        return PAGE_EXISTS;
    }

}

fun main(args: Array<String>)
{
    val f = JFrame("Image 6up")
    val g = gui()
    f.contentPane = g.panel
    f.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    f.setSize(800, 500)
    f.isResizable = true
    f.setLocationRelativeTo(null)
    f.isVisible = true

    g.list1.visibleRowCount = 3
    g.list1.layoutOrientation = JList.VERTICAL_WRAP
    g.list1.setCellRenderer(customListCellRenderer())

    g.list1.setListData(imagesPanel)

    // リストをWクリックで削除
    g.list1.addMouseListener(object : MouseAdapter()
    {
        override fun mouseClicked(evt: MouseEvent)
        {
            val list = evt.getSource() as JList<*>
            if (evt.getClickCount() === 2)
            {
                val index = list.locationToIndex(evt.getPoint())
                imagesPanel.removeElementAt(index)
                images.removeElementAt(index)
                g.list1.setListData(imagesPanel)
            }
        }
    })


    // Pasteボタン clipboardから読み込みリストに追加
    g.readClipboardButton.addActionListener {
        if (images.count() >= 6) return@addActionListener

        var clip: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        var data = clip.getContents(null)
        if (!data.isDataFlavorSupported(DataFlavor.imageFlavor)) return@addActionListener

        var img = clip.getData(DataFlavor.imageFlavor) as Image
        var oneline = JPanel()
        oneline.preferredSize = Dimension(80, 80)
        oneline.add(JLabel(ImageIcon(img.getScaledInstance(80, 80, Image.SCALE_DEFAULT))))
        images.addElement(img)
        imagesPanel.addElement(oneline)
        g.list1.setListData(imagesPanel)
    }

    // Printボタン リストを成形し印刷
    g.printButton.addActionListener {
        if (images.count() <= 0) return@addActionListener
        var rqset = HashPrintJobAttributeSet()
        rqset.add(Copies(1))
        rqset.add(MediaSizeName.ISO_A4)
        rqset.add(MediaPrintableArea(10.1f, 10.3f, 189.8f, 276.4f, MediaPrintableArea.MM))
        var pj = PrinterJob.getPrinterJob()
        pj.setPrintable(printOnePage())
        if (pj.printDialog())
        {
            try
            {
                pj.print()
            } catch (e: PrinterException)
            {
                System.err.println(e)
            }
        }
    }

}


