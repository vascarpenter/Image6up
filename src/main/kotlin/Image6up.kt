/*
    Image6up

    画像を6枚分貼り付けて印刷するために整形するプログラム
    え？ Wordのplaceholderでできるって？

    Copyright © 2021 gikoha

    use imgscalr - Java Image-Scaling Library: https://github.com/rkalla/imgscalr
 */

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.imgscalr.Scalr
import java.awt.*
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.Printable.NO_SUCH_PAGE
import java.awt.print.Printable.PAGE_EXISTS
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.io.File
import java.io.IOException
import java.util.*
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.Copies
import javax.print.attribute.standard.MediaPrintableArea
import javax.print.attribute.standard.MediaSizeName
import javax.swing.*


var imagesPanel: Vector<JPanel> = Vector<JPanel>()
var images: Vector<Image> = Vector<Image>()
var nup: Int = 2

data class PageElement(var imgSize: Float, var xnum: Int, var ynum: Int)

var pagenum: Vector<PageElement> = Vector<PageElement>()

class CustomListCellRenderer : DefaultListCellRenderer()
{
    override fun getListCellRendererComponent(
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

class PrintOnePage : Printable
{
    override fun print(graphics: Graphics?, pageFormat: PageFormat?, pageIndex: Int): Int
    {
        if (pageIndex != 0) return NO_SUCH_PAGE
        val g2 = graphics as Graphics2D
        //        g2.setFont(Font(DIALOG,PLAIN,12))
        //        g2.setStroke(BasicStroke(0.5f ))
        //        g2.drawString("ogehage",0,0)
        for ((j, image) in images.withIndex())
        {
            if(j>=pagenum[nup].xnum*pagenum[nup].ynum)
                break
            val xx = pagenum[nup].imgSize * (j / pagenum[nup].ynum) + 10
            val yy = pagenum[nup].imgSize * (j % pagenum[nup].ynum) + 10
            val newimg = Scalr.resize(
                createBufferedImage(image),
                (pagenum[nup].imgSize - 10).toInt()
            ) // resize with keeping aspect ratio
            g2.drawImage(newimg, xx.toInt(), yy.toInt(), null)
        }
        return PAGE_EXISTS
    }

}

// Image から BufferedImageへの変換
//  https://www.ne.jp/asahi/hishidama/home/tech/java/image.html

fun createBufferedImage(img: Image): BufferedImage
{
    val bimg = BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB)
    val g = bimg.graphics
    g.drawImage(img, 0, 0, null)
    g.dispose()
    return bimg
}

fun pasteImage(g: gui)
{
    if (images.count() >= 6) return

    val clip: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val data = clip.getContents(null)
    if (!data.isDataFlavorSupported(DataFlavor.imageFlavor)) return

    val img = clip.getData(DataFlavor.imageFlavor) as Image
    val oneline = JPanel()
    oneline.preferredSize = Dimension(110, 110)
    val newimg = Scalr.resize(createBufferedImage(img), 100)
    oneline.add(JLabel(ImageIcon(newimg)))
    images.addElement(img)
    imagesPanel.addElement(oneline)
    g.list1.setListData(imagesPanel)
}

fun printImage(g: gui)
{
    if (images.count() <= 0) return
    val printAttr = HashPrintRequestAttributeSet()
    printAttr.add(Copies(1))
    printAttr.add(MediaSizeName.ISO_A4)
    printAttr.add(MediaPrintableArea(10.1f, 10.3f, 189.8f, 276.4f, MediaPrintableArea.MM))
    val pj = PrinterJob.getPrinterJob()
    pj.setPrintable(PrintOnePage())
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

fun savePDF(filePath: String)
{
    try
    {
        // 空のドキュメントオブジェクトを作成します
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)
        val contentStream = PDPageContentStream(document, page)
        for ((j, image) in images.withIndex())
        {
            if(j>=pagenum[nup].xnum*pagenum[nup].ynum)
                break
            val xx: Float = pagenum[nup].imgSize * (j / pagenum[nup].ynum) + 10
            val yy: Float =
                pagenum[nup].imgSize * pagenum[nup].ynum - (pagenum[nup].imgSize * (j % pagenum[nup].ynum) + 10) - pagenum[nup].imgSize + 40  // PDFはy軸が逆 位置調整20210716
            val newimg = Scalr.resize(
                createBufferedImage(image),
                ((pagenum[nup].imgSize - 10) * 2).toInt()
            ) // resize with keeping aspect ratio
            val pdfimg = JPEGFactory.createFromImage(document, newimg, 1.0f, 300)
            contentStream.drawImage(pdfimg, xx, yy, pdfimg.width.toFloat() / 2, pdfimg.height.toFloat() / 2)
        }
        contentStream.close()
        document.save(filePath)
        document.close()
    } catch (e: IOException)
    {
        e.printStackTrace()
    }
}

fun setnup(g: gui)
{
    nup = when
    {
        g.radio1Button.isSelected -> 0
        g.radio4Button.isSelected -> 1
        else -> 2
    }
}

fun main(args: Array<String>)
{
    val f = JFrame("Image 6up")
    val g = gui()

    pagenum.add(PageElement(500f, 1, 1))
    pagenum.add(PageElement(250f, 2, 2))
    pagenum.add(PageElement(250f, 2, 3))

    val menub = JMenuBar()

    // メニューからの Paste : CTRL+Vも追加
    val menuitem = JMenuItem("Paste")
    menuitem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK)
    menuitem.mnemonic = KeyEvent.VK_P
    menuitem.addActionListener {
        pasteImage(g)
    }

    // メニューからの 印刷
    val menuitem1 = JMenuItem("Print")
    menuitem1.addActionListener {
        setnup(g)
        printImage(g)
    }

    val menu1 = JMenu("File")
    val menu2 = JMenu("Edit")
    menu1.mnemonic = KeyEvent.VK_F
    menu1.add(menuitem1)
    menu2.mnemonic = KeyEvent.VK_E
    menu2.add(menuitem)
    menub.add(menu1)
    menub.add(menu2)
    f.jMenuBar = menub

    g.readClipboardButton.mnemonic = KeyEvent.VK_P
    f.contentPane = g.panel
    f.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    f.setSize(600, 500)
    f.isResizable = true
    f.setLocationRelativeTo(null)
    f.isVisible = true

    val buttonGroup = ButtonGroup()
    buttonGroup.add(g.radio1Button)
    buttonGroup.add(g.radio4Button)
    buttonGroup.add(g.radio6Button)

    g.list1.visibleRowCount = 3
    g.list1.layoutOrientation = JList.VERTICAL_WRAP
    g.list1.cellRenderer = CustomListCellRenderer()

    g.list1.setListData(imagesPanel)


    // リストをWクリックで削除
    g.list1.addMouseListener(object : MouseAdapter()
    {
        override fun mouseClicked(evt: MouseEvent)
        {
            val list = evt.source as JList<*>
            if (evt.clickCount == 2)
            {
                if (images.count() <= 0)
                    return      // imageがないなら削除できないね
                val index = list.locationToIndex(evt.point)
                imagesPanel.removeElementAt(index)
                images.removeElementAt(index)
                g.list1.setListData(imagesPanel)
            }
        }
    })


    // Pasteボタン clipboardから読み込みリストに追加
    g.readClipboardButton.addActionListener {
        pasteImage(g)
    }

    // Printボタン リストを成形し印刷
    g.printButton.addActionListener {
        setnup(g)
        printImage(g)
    }

    g.savePDFButton.addActionListener {
        setnup(g)
        val fc = JFileChooser()
        fc.selectedFile = File("image.pdf")
        val returnVal = fc.showSaveDialog(f)
        if (returnVal == JFileChooser.CANCEL_OPTION || returnVal == JFileChooser.ERROR_OPTION)
            return@addActionListener

        savePDF(fc.selectedFile.absolutePath)
    }
}


