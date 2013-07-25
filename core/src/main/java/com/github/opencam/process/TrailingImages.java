package com.github.opencam.process;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.github.opencam.imagegrabber.Resource;

public class TrailingImages {
  Map<String, LinkedList<Resource>> trailingImages = new HashMap<String, LinkedList<Resource>>();
  Map<String, LinkedList<ProcessedImageHolder>> processedImages = new HashMap<String, LinkedList<ProcessedImageHolder>>();
  int maxTrail;
  int processTrail;

  public TrailingImages(final int maxTrail, final int processedTrail) {
    super();
    this.maxTrail = maxTrail;
    this.processTrail = processedTrail;
  }

  public synchronized void addImage(final String cameraName, final Resource image) {
    LinkedList<Resource> linkedList = trailingImages.get(cameraName);

    if (linkedList == null) {
      linkedList = new LinkedList<Resource>();
      trailingImages.put(cameraName, linkedList);
    }

    linkedList.addLast(image);
    if (linkedList.size() > maxTrail) {
      linkedList.removeFirst();
    }
  }

  public synchronized List<Resource> getTrailingImages(final String cameraName) {

    final LinkedList<Resource> linkedList = trailingImages.get(cameraName);
    if (linkedList == null) {
      return new LinkedList<Resource>();
    } else {
      return new ArrayList<Resource>(linkedList);
    }
  }

  public synchronized void addProcessedImage(final String cameraName, final Resource image, final BufferedImage img) {
    LinkedList<ProcessedImageHolder> linkedList = processedImages.get(cameraName);

    if (linkedList == null) {
      linkedList = new LinkedList<ProcessedImageHolder>();
      processedImages.put(cameraName, linkedList);
    }

    final ProcessedImageHolder lastProcessed = new ProcessedImageHolder(image, img);
    linkedList.addLast(lastProcessed);

    if (linkedList.size() > processTrail) {
      linkedList.removeFirst();
    }
  }

  public synchronized List<ProcessedImageHolder> getTrailingProcessedImages(final String cameraName) {
    final LinkedList<ProcessedImageHolder> linkedList = processedImages.get(cameraName);

    if (linkedList == null) {
      return new LinkedList<ProcessedImageHolder>();
    } else {
      return new ArrayList<ProcessedImageHolder>(linkedList);
    }
  }

  public ProcessedImageHolder getLastProcessedImage(final String cameraName) {
    final List<ProcessedImageHolder> list = getTrailingProcessedImages(cameraName);
    if (list == null || list.size() < 1) {
      return null;
    }

    return list.get(list.size() - 1);

  }

  public Resource getLastImage(final String cameraName) {
    final List<Resource> list = getTrailingImages(cameraName);
    if (list == null || list.size() < 1) {
      return null;
    }

    return list.get(list.size() - 1);
  }
}
