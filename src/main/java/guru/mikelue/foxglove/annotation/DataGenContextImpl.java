package guru.mikelue.foxglove.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.mikelue.foxglove.DataGenerator;
import guru.mikelue.foxglove.TableFacet;
import guru.mikelue.foxglove.functional.DataGeneratorProvider;
import guru.mikelue.foxglove.functional.TableFacetsProvider;

class DataGenContextImpl<T extends TableFacet> implements DataGenContext<T> {
	private final SupplierFactory<TableFacetsSource, List<T>>
		tableFacetsSuppliers = new SupplierFactoryOfTableFacet<>();

	private final SupplierFactory<DataGeneratorSource, DataGenerator<T>>
		dataGeneratorSuppliers = new SupplierFactoryOfDataGenerator<>();

	DataGenContextImpl(Class<?> testClass)
	{
		Predicate<Member> notPrivateMethodOfSuperclass = m -> {
			var declaringClass = m.getDeclaringClass();

			// Same class always allowed
			if (testClass.isAssignableFrom(declaringClass)) {
				return true;
			}

			var modifiers = m.getModifiers();
			// Public/protected methods of superclass allowed
			if ((modifiers & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0) {
				return true;
			}

			if ((modifiers & Modifier.PRIVATE) != 0) {
				return false;
			}

			return testClass.getPackageName()
				.equals(declaringClass.getPackageName());
		};

		MethodUtils.getMethodsListWithAnnotation(
			testClass, TableFacetsSource.class, true, true
		)
			.stream()
			.filter(notPrivateMethodOfSuperclass)
			.forEach(m -> {
				tableFacetsSuppliers.addMethod(m);
			});

		FieldUtils.getFieldsListWithAnnotation(
			testClass, TableFacetsSource.class
		)
			.stream()
			.filter(notPrivateMethodOfSuperclass)
			.forEach(m -> {
				tableFacetsSuppliers.addField(m);
			});

		MethodUtils.getMethodsListWithAnnotation(
			testClass, DataGeneratorSource.class, true, true
		)
			.stream()
			.filter(notPrivateMethodOfSuperclass)
			.forEach(m -> {
				dataGeneratorSuppliers.addMethod(m);
			});

		FieldUtils.getFieldsListWithAnnotation(
			testClass, DataGeneratorSource.class
		)
			.stream()
			.filter(notPrivateMethodOfSuperclass)
			.forEach(m -> {
				dataGeneratorSuppliers.addField(m);
			});
	}

	@Override
	public List<T> getTypedTableFacets(Class<? extends TableFacetsProvider<T>> clazz, Object testingInstance)
	{
		return initializeProvider(clazz, testingInstance)
			.get();
	}

	@Override
	public List<T> getNamedTableFacets(String name, Object testingInstance)
	{
		return tableFacetsSuppliers.grabInstance(name, testingInstance);
	}

	@Override
	public DataGenerator<T> getTypedDataGenerator(Class<? extends DataGeneratorProvider<T>> clazz, Object testingInstance)
	{
		return initializeProvider(clazz, testingInstance)
			.get();
	}

	@Override
	public DataGenerator<T> getNamedDataGenerator(String name, Object testingInstance)
	{
		return dataGeneratorSuppliers.grabInstance(name, testingInstance);
	}

	@Override
	public Optional<List<T>> getDefaultTableFacets(Object testingInstance)
	{
		return Optional.ofNullable(
			tableFacetsSuppliers.grabSingleInstance(testingInstance)
		);
	}

	@Override
	public Optional<DataGenerator<T>> getDefaultDataGenerator(Object testingInstance)
	{
		return Optional.ofNullable(
			dataGeneratorSuppliers.grabSingleInstance(testingInstance)
		);
	}

	boolean hasNamedTableFacets(String name)
	{
		return tableFacetsSuppliers.hasSource(name);
	}

	boolean hasNamedDataGenerator(String name)
	{
		return dataGeneratorSuppliers.hasSource(name);
	}

	private static <T> T initializeProvider(
		Class<T> clazz, Object testingInstance
	) {
		try {
			Constructor<T> constructor = getConstructorOf(clazz, testingInstance.getClass());
			if (Modifier.isStatic(clazz.getModifiers()) ||
				!clazz.isMemberClass()
			) {
				return constructor.newInstance();
			}

			return constructor.newInstance(testingInstance);
		} catch (Exception e) {
			throw new RuntimeException(
				String.format("Failed to instantiate Provider<%s>.",
					clazz.getSimpleName()
				),
				e
			);
		}
	}

	private static <T> Constructor<T> getConstructorOf(
		Class<T> targetClass, Class<?> enclosingClass
	) throws NoSuchMethodException {
		Constructor<T> constructor = null;

		if (
			Modifier.isStatic(targetClass.getModifiers()) ||
			!targetClass.isMemberClass()
		) {
			constructor = targetClass.getDeclaredConstructor(new Class<?>[0]);
		} else {
			constructor = targetClass.getDeclaredConstructor(enclosingClass);
		}

		constructor.trySetAccessible();
		return constructor;
	}
}

abstract class SupplierFactory<A extends Annotation, T> {
	protected final static Logger logger = LoggerFactory.getLogger(SupplierFactory.class);

	private Map<String, FailableFunction<Object, T, Throwable>> functionOfSources = new HashMap<>();

	private Function<A, String> nameExtractor;
	protected Class<A> annotationType;

	SupplierFactory(
		Class<A> annotationType, Function<A, String> nameExtractor
	) {
		this.annotationType = annotationType;
		this.nameExtractor = nameExtractor;
	}

	void addMethod(Method method)
	{
		var memberAccessor = new MethodAccessor<A>(method, annotationType, nameExtractor);

		Validate.isTrue(
			method.getParameterCount() == 0,
			"Method<%s> should not have parameters.", memberAccessor
		);

		addMember(memberAccessor);
	}

	void addField(Field field)
	{
		addMember(new FieldAccessor<>(field, annotationType, nameExtractor));
	}

	private void addMember(MemberAccessor<?> memberAccessor)
	{
		validateAddedName(memberAccessor);
		validateSupportedType(memberAccessor);

		var name = memberAccessor.getProviderName();
		functionOfSources.put(name, buildGrabFunction(memberAccessor));

		if (logger.isDebugEnabled()) {
			var modifiers = Modifier.toString(memberAccessor.getMember().getModifiers());
			logger.debug("Adding source[{}]. <{}> {}.",
				name, modifiers, memberAccessor
			);
		}
	}

	boolean hasSource(String name)
	{
		return functionOfSources.containsKey(name);
	}

	T grabInstance(String name, Object testingInstance)
	{
		Validate.isTrue(
			functionOfSources.containsKey(name),
			"No @%s found for name: [%s]",
			annotationType.getSimpleName(), name
		);

		try {
			return functionOfSources.get(name).apply(testingInstance);
		} catch (Throwable t) {
			throw new RuntimeException(
				String.format("Failed to execute @%s method for name: [%s]",
					annotationType.getSimpleName(), name
				), t
			);
		}
	}

	T grabSingleInstance(Object testingInstance)
	{
		int numberOfSources = functionOfSources.size();

		Validate.isTrue(numberOfSources <= 1,
			"Multiple members annotated by @%s are defined. Needs only one for default object resolving.",
			annotationType.getSimpleName()
		);

		if (numberOfSources == 0) {
			return null;
		}

		try {
			return functionOfSources.values().iterator().next()
				.apply(testingInstance);
		} catch (Throwable t) {
			throw new RuntimeException(
				String.format("Failed to get single @%s source",
					annotationType.getSimpleName()
				), t
			);
		}
	}

	abstract protected void validateSupportedType(MemberAccessor<?> memberAccessor);

	abstract protected FailableFunction<Object, T, Throwable> buildGrabFunction(MemberAccessor<?> memberAccessor);

	private void validateAddedName(MemberAccessor<?> memberAccessor)
	{
		var name = memberAccessor.getProviderName();

		Validate.isTrue(
			!functionOfSources.containsKey(name),
			"Duplicated: %s", memberAccessor
		);
	}
}

class SupplierFactoryOfDataGenerator<T extends TableFacet> extends SupplierFactory<DataGeneratorSource, DataGenerator<T>> {
	SupplierFactoryOfDataGenerator()
	{
		super(DataGeneratorSource.class, DataGeneratorSource::value);
	}

	@Override
	protected void validateSupportedType(MemberAccessor<?> memberAccessor)
	{
		var checkedType = memberAccessor.getSignificantType();

		Validate.isTrue(
			DataGenerator.class.isAssignableFrom(checkedType) ||
			DataGeneratorProvider.class.isAssignableFrom(checkedType),
			"Member<%s> should be type of %2$s, DataGeneratorProvider<? extends TableFacet>(just for field). But got: \"%3$s\".",
			memberAccessor, DataGenerator.class.getSimpleName(),
			memberAccessor.getSignificantType().getSimpleName()
		);
	}

	@Override
	protected FailableFunction<Object, DataGenerator<T>, Throwable> buildGrabFunction(MemberAccessor<?> memberAccessor)
	{
		var checkedType = memberAccessor.getSignificantType();

		if (DataGeneratorProvider.class.isAssignableFrom(checkedType)) {
			return instance -> memberAccessor.<DataGeneratorProvider<T>>grabValue(instance)
				.get();
		}

		return testingInstance -> {
			return memberAccessor.grabValue(testingInstance);
		};
	}
}

class SupplierFactoryOfTableFacet<T extends TableFacet> extends SupplierFactory<TableFacetsSource, List<T>> {
	SupplierFactoryOfTableFacet()
	{
		super(TableFacetsSource.class, TableFacetsSource::value);
	}

	@Override
	protected void validateSupportedType(MemberAccessor<?> memberAccessor)
	{
		var checkedType = memberAccessor.getSignificantType();

		if (checkedType.isArray()) {
			checkedType = checkedType.getComponentType();
		} else if (
			checkedType.isAssignableFrom(List.class) ||
			checkedType.isAssignableFrom(Stream.class) ||
			( // Field supporting TableFacetsProvider<T>
				memberAccessor instanceof FieldAccessor &&
				TableFacetsProvider.class.isAssignableFrom(checkedType)
			)
		) {
			checkedType = memberAccessor.getGenericTypeOfFirstParameter();
		}

		Validate.isTrue(
			TableFacet.class.isAssignableFrom(checkedType),
			"Member<%s> should be type of %2$s, List<%2$s>, Stream<%2$s>, %2$s[], or TableFacetsProvider<%2$s>(just for field). But got: \"%3$s\".",
			memberAccessor, TableFacet.class.getSimpleName(),
			memberAccessor.getSignificantType().getSimpleName()
		);
	}

	@Override
	protected FailableFunction<Object, List<T>, Throwable> buildGrabFunction(MemberAccessor<?> memberAccessor)
	{
		var checkedType = memberAccessor.getSignificantType();

		if (TableFacet.class.isAssignableFrom(checkedType)) {
			return testingInstance -> {
				T o = memberAccessor.grabValue(testingInstance);
				return List.of(o);
			};
		}

		if (List.class.isAssignableFrom(checkedType)) {
			return memberAccessor::grabValue;
		}

		if (Stream.class.isAssignableFrom(checkedType)) {
			return testingInstance -> {
				Stream<T> streamOfFacets = memberAccessor.grabValue(testingInstance);
				return streamOfFacets.toList();
			};
		}

		if (TableFacetsProvider.class.isAssignableFrom(checkedType)) {
			return instance -> memberAccessor.<TableFacetsProvider<T>>grabValue(instance)
				.get();
		}

		return testingInstance -> {
			T[] arrayOfFacets = memberAccessor.grabValue(testingInstance);
			return List.of(arrayOfFacets);
		};
	}
}

interface MemberAccessor<M extends Member & AnnotatedElement> {
	Class<?> getSignificantType();
	Class<?> getGenericTypeOfFirstParameter();
	<T> T grabValue(Object instance) throws Throwable;
	M getMember();
	String getProviderName();
}

abstract class AbstractMemberAccessor<M extends Member & AnnotatedElement, A extends Annotation> implements MemberAccessor<M> {
	private final Class<A> annotationType;
	private final Function<A, String> nameExtractor;
	private FailableFunction<Object, ?, Throwable> invoker;

	AbstractMemberAccessor(Class<A> annotationType, Function<A, String> nameExtractor)
	{
		this.annotationType = annotationType;
		this.nameExtractor = nameExtractor;
	}

	@Override
	public String getProviderName()
	{
		var member = getMember();
		var name = nameExtractor.apply(
			member.getAnnotation(annotationType)
		);

		return !name.isEmpty() ? name : member.getName();
	}

	@Override
	public String toString()
	{
		var member = getMember();

		return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
			.append("@" + annotationType.getSimpleName())
			.append(String.format("<%s> %s.%s",
				member.getClass().getSimpleName(),
				member.getDeclaringClass().getSimpleName(),
				member.getName()
			))
			.toString();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T grabValue(Object instance) throws Throwable
	{
		return (T)invoker.apply(instance);
	}

	final protected void setInvoker(FailableFunction<Object, ?, Throwable> invoker)
	{
		this.invoker = invoker;
	}
}

class MethodAccessor<A extends Annotation> extends AbstractMemberAccessor<Method, A> {
	private final Method method;

	MethodAccessor(Method method, Class<A> annotationType, Function<A, String> nameExtractor)
	{
		super(annotationType, nameExtractor);
		this.method = method;

		FailableFunction<Object, ?, Throwable> invoker;
		if (Modifier.isStatic(method.getModifiers())) {
			invoker = instance -> {
				var oldValue = method.canAccess(null);
				method.setAccessible(true);

				try {
					return method.invoke(null);
				} finally {
					method.setAccessible(oldValue);
				}
			};
		} else {
			invoker = instance ->
				MethodUtils.invokeMethod(
					instance, true, method.getName()
				);
		}

		setInvoker(invoker);
	}

	@Override
	public Class<?> getSignificantType()
	{
		return method.getReturnType();
	}

	@Override
	public Class<?> getGenericTypeOfFirstParameter()
	{
		var genericType = (ParameterizedType)method.getGenericReturnType();
		return (Class<?>)genericType.getActualTypeArguments()[0];
	}

	@Override
	public Method getMember()
	{
		return method;
	}
}

class FieldAccessor<A extends Annotation> extends AbstractMemberAccessor<Field, A> {
	private final Field field;

	FieldAccessor(Field field, Class<A> annotationType, Function<A, String> nameExtractor)
	{
		super(annotationType, nameExtractor);

		this.field = field;

		FailableFunction<Object, ?, Throwable> invoker;
		if (Modifier.isStatic(field.getModifiers())) {
			invoker = instance -> {
				return FieldUtils.readStaticField(field, true);
			};
		} else {
			invoker = instance -> {
				return FieldUtils.readField(field, instance, true);
			};
		}

		setInvoker(invoker);
	}

	@Override
	public Class<?> getSignificantType()
	{
		return field.getType();
	}

	@Override
	public Class<?> getGenericTypeOfFirstParameter()
	{
		var genericType = (ParameterizedType)field.getGenericType();
		return (Class<?>)genericType.getActualTypeArguments()[0];
	}

	@Override
	public Field getMember()
	{
		return field;
	}
}
