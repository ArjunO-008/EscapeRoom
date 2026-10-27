Background music
================

Put your game music in this folder as a .wav file.

Which file is played, and how loud, is set in ONE place:
    src/audio/AudioConfig.java   ->  MUSIC_FILE, MUSIC_VOLUME, MUSIC_LOOP_CROSSFADE_MS

The music starts as soon as the game opens and loops forever. The end of the
track is blended into its beginning (MUSIC_LOOP_CROSSFADE_MS) so the repeat is
not noticeable. Set it to 0 for a plain loop.

(If MUSIC_FILE does not exist, the first .wav/.aiff/.au in this folder is used.)

Java can only play WAV / AIFF / AU without extra libraries, so if your music
is an .mp3 or .ogg, convert it first, for example with ffmpeg:

    ffmpeg -i mymusic.mp3 -ar 44100 -ac 2 bgm.wav

(Audacity: File > Export > Export as WAV also works.)

Change the volume while the game runs from code:
    SoundManager.setMusicVolume(0.2);

Press M in the game to mute / unmute everything, music included.
