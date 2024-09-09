### Method Used For Streaming
- We identify piecies of video file inside torrent.
- We download first 5 and last piecies resulting in + ---------- +
- We continue downloading sequential from the start
- Setting 5 first incomplete piecies with TOP_PRIORITY
- Setting 5 first incomplete piecies that dont have TOP_PRIORITY to DEFAULT_PRIORITY
- Setting 5 first incomplete piecies that dont have TOP_PRIORITY & DEFAULT_PRIORITY TO THREE_PRIORITY

Resulting In

TOP PRIORITY PIECIES -> |||||  <br/> 
DEFAULT PRIORITY PIECIES -> |||||  <br/>             
THREE PRIORITY PIECIES -> |||||  

++----------+ <br/> 
+++---------+ <br/> 
++++--------+ <br/> 
+++++-------+

### TODO
1. CREATE HTTP SERVER TO SERVE VIDEO FILE BASED ON PATH.
2. FIX BROKEN PIPE ON HTTP SERVER WHILE SERVING WITH VLC (IF POSSIBLE)

### Technologies Used
- VLC
- Libtorrent4j
- Java

### Interfaces

<div style="display: flex;">
<img src="https://github.com/Xristosxmp/torstream/blob/main/assets/Screenshot_20240905_122752.png" width="200">
<img src="https://github.com/Xristosxmp/torstream/blob/main/assets/Screenshot_20240905_122805.png" width="200">

https://github.com/user-attachments/assets/2781db2f-44e3-46a3-b185-75378d2db5a6




</div>
