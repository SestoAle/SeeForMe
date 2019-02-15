

<p align="center">
<img  src="https://i.imgur.com/QIFLKO0.png" width="60%" height="60%"/>
</p>

A mobile App for the automatic recognition of museum artworks and the semi-automatic management of multimedia feedback.

The project aims to realize a prototype of a **smart-interface** for a musuem audio-guide. The app is based on an 
automatic recognition system previously created but not yet imported in Java that uses machine-learning to recognize artworks and people.

The app:
* can recognize museum artworks;
* can automatically understand the context: people occlusion, distance and *importance* of artworks;
* can create multimedia cards of the visited artworks;
* can offer semi-automatic control of multimedia feedbacks.

The prototype is Android native and java only.

The project was part of a Bachelor thesis in Computer Engineering at Università degli Studi di Firenze, with title " A mobile App for the automatic recognition of museum artworks and the semiautomatic management of multimedia feedback".

## Interface
Here are some examples of the prototype interface:

<p align="center">
<img  src="https://i.imgur.com/qCa9wS1.png" width="80%" height="80%"/>
</p>

<p align="center">
<img  src="https://i.imgur.com/rw2Uz6U.png" width="70%" height="70%"/>
</p>

## Implementation
The application architecture is shown in figure:

<p align="center">
<img  src="https://i.imgur.com/L1IWNNp.png" width="80%" height="80%"/>
</p>

The artwork databased is within the code and it's limited to the Bargello Florence museum artworks. The app lets the user
to frame a statue and then he can listen to the audio-guide of the recognized work; the listened works are saved to the 
history section so the user can re-watch the description later. The app allows automatic, semi-automatic or manual controls: 
the automatic controls are possible thanks to the recognition system that outputs useful informations about the behavior 
of the user.

## Report
A copy of the presentation (italian) can be found
<a href="https://github.com/SestoAle/SeeForMe/raw/master/documents/presentation.pdf" download="presentation.pdf">here</a>.

## License
Licensed under the term of [MIT License](https://github.com/SeeForMe/DeepCrawl/blob/master/LICENSE).
