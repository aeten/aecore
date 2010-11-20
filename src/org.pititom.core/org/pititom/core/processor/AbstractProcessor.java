package org.pititom.core.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.Diagnostic.Kind;

import org.pititom.core.logging.LogLevel;

public abstract class AbstractProcessor extends javax.annotation.processing.AbstractProcessor {
	public static enum WriteMode {
		CREATE, APPEND, OVERRIDE
	}

	protected volatile LogLevel logLevel = LogLevel.INFO;

	public static PrintWriter getWriter(FileObject fileObject, WriteMode mode, boolean autoFlush) throws IllegalArgumentException, IOException {
		try {
			File file = getFile(fileObject);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
			}
			if (file.exists() && !file.isDirectory() && (mode == WriteMode.CREATE)) {
				throw new IOException("File \"" + file + "\" already exist");
			}

			if (mode == WriteMode.APPEND) {
				return new PrintWriter(new FileWriter(file, false));
				// return new PrintWriter(fileObject.openOutputStream(),
				// autoFlush);
				// try {
				// BufferedReader reader = getReader(fileObject);
				// StringWriter copy = new StringWriter();
				// PrintWriter writer = new PrintWriter(copy);
				// String line;
				// while((line = reader.readLine()) != null) {
				// writer.println(line);
				// }
				// // writer = new PrintWriter(fileObject.openOutputStream(),
				// autoFlush);
				// writer = new PrintWriter(fileObject.openWriter(), autoFlush);
				// writer.write(copy.toString());
				// return writer;
				// } catch (FileNotFoundException exception) {
				//
				// }
			}
			return new PrintWriter(fileObject.openOutputStream(), autoFlush);
			// return new PrintWriter(fileObject.openWriter(), autoFlush);
			// return new PrintWriter(new FileWriter(file, mode ==
			// WriteMode.APPEND), autoFlush);
		} catch (UnsupportedOperationException exception) {
			return new PrintWriter(fileObject.openOutputStream(), autoFlush);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException(fileObject.toString(), exception);
		}
	}

	public static BufferedReader getReader(FileObject fileObject) throws FileNotFoundException, IllegalArgumentException, IOException {
		return new BufferedReader(new FileReader(getFile(fileObject)));
	}

	protected static File getFile(FileObject fileObject) {
		URI uri = fileObject.toUri();
		if (uri.isAbsolute()) {
			return new File(uri);
		}
		return new File(uri.toString());
	}

	protected AnnotationMirror getAnnotationMirror(Element element, Class<? extends Annotation> annotationClass) {
		for (AnnotationMirror annotation : processingEnv.getElementUtils().getAllAnnotationMirrors(element)) {
			if (annotationMirrorMatches(annotation, annotationClass)) {
				return annotation;
			}
		}
		return null;
	}

	protected static boolean annotationMirrorMatches(AnnotationMirror annotation, Class<? extends Annotation> annotationClass) {
		Name qualifiedName = ((TypeElement) (annotation.getAnnotationType()).asElement()).getQualifiedName();
		return qualifiedName.contentEquals(annotationClass.getName());
	}

	protected static TypeElement toElement(AnnotationValue value) {
		return (TypeElement) ((DeclaredType) ((TypeMirror) value.getValue())).asElement();
	}

	protected List<AnnotationMirror> getAnnotationMirrors(Element element, Class<? extends Annotation> annotationClass) {
		List<AnnotationMirror> annotationMirrors = new ArrayList<AnnotationMirror>();
		for (AnnotationMirror annotation : processingEnv.getElementUtils().getAllAnnotationMirrors(element)) {
			if (annotationMirrorMatches(annotation, annotationClass)) {
				annotationMirrors.add(annotation);
			}
		}
		return annotationMirrors;
	}

	protected static Collection<AnnotationValue> findValue(AnnotationMirror mirror, String key) {
		Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
			if (entry.getKey().getSimpleName().contentEquals(key)) {
				@SuppressWarnings("unchecked")
				Collection<AnnotationValue> result = (Collection<AnnotationValue>) entry.getValue().getValue();
				return result;
			}
		}
		throw new IllegalStateException("No value found in element");
	}

	protected static Collection<AnnotationValue> findValue(AnnotationMirror annotation) {
		return findValue(annotation, "value");
	}

	protected AnnotationValue getAnnotationValue(AnnotationMirror annotation, String key) {
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : processingEnv.getElementUtils().getElementValuesWithDefaults(annotation).entrySet()) {
			if (entry.getKey().getSimpleName().contentEquals(key)) {
				return entry.getValue();
			}
		}
		return null;
	}

	protected AnnotationValue getAnnotationValue(AnnotationMirror annotation) {
		return getAnnotationValue(annotation, "value");
	}

	protected AnnotationValue getAnnotationValue(Element element, Class<? extends Annotation> annotation, String key) {
		for (AnnotationMirror confAnnotation : getAnnotationMirrors(element, annotation)) {
			for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> value : confAnnotation.getElementValues().entrySet()) {
				if (value.getKey().getSimpleName().contentEquals(key)) {
					return value.getValue();
				}
			}
		}
		return null;
	}

	protected AnnotationValue getAnnotationValue(Element element, Class<? extends Annotation> annotation) {
		return getAnnotationValue(element, annotation, "value");
	}

	protected String getProperQualifiedName(TypeElement provider) {
		return processingEnv.getElementUtils().getBinaryName(provider).toString();
	}

	protected void debug(String message) {
		if (this.logLevel.compareTo(LogLevel.DEBUG) <= 0)
			processingEnv.getMessager().printMessage(Kind.NOTE, message);
	}

	protected void note(String message) {
		if (this.logLevel.compareTo(LogLevel.INFO) <= 0)
			processingEnv.getMessager().printMessage(Kind.NOTE, message);
	}

	protected void warn(String message, Element element, AnnotationMirror annotation, AnnotationValue value) {
		if (this.logLevel.compareTo(LogLevel.WARN) <= 0)
			processingEnv.getMessager().printMessage(Kind.WARNING, message, element, annotation, value);
	}

	protected void warn(String message, Element element, AnnotationMirror annotation) {
		if (this.logLevel.compareTo(LogLevel.WARN) <= 0)
			processingEnv.getMessager().printMessage(Kind.WARNING, message, element, annotation);
	}

	protected void warn(String message, Element element) {
		if (this.logLevel.compareTo(LogLevel.WARN) <= 0)
			processingEnv.getMessager().printMessage(Kind.WARNING, message, element);

	}

	protected void error(String message, Element element, AnnotationMirror annotation, AnnotationValue value) {
		processingEnv.getMessager().printMessage(Kind.ERROR, message, element, annotation, value);
	}

	protected void error(String message, Element element, AnnotationMirror annotation) {
		processingEnv.getMessager().printMessage(Kind.ERROR, message, element, annotation);
	}

	protected void error(String message, Element element) {
		processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
	}

	protected void error(String message, Throwable cause, Element element, AnnotationMirror annotation, AnnotationValue value) {
		processingEnv.getMessager().printMessage(Kind.ERROR, message + "\nCaused by " + cause + ":\n" + getStackTrace(cause), element, annotation, value);
	}

	protected void error(String message, Throwable cause, Element element, AnnotationMirror annotation) {
		processingEnv.getMessager().printMessage(Kind.ERROR, message + "\nCaused by " + cause + ":\n" + getStackTrace(cause), element, annotation);
	}

	protected void error(String message, Throwable cause, Element element) {
		processingEnv.getMessager().printMessage(Kind.ERROR, message + "\nCaused by " + cause + ":\n" + getStackTrace(cause), element);
	}

	private static String getStackTrace(Throwable cause) {
		StringWriter stackTrace = new StringWriter();
		cause.printStackTrace(new PrintWriter(stackTrace));
		return stackTrace.toString();
	}
}