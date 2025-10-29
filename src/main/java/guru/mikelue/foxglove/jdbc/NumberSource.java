package guru.mikelue.foxglove.jdbc;

enum NumberSource {
	Plain("numberOfRows() has been used"),
	KeyColumn("key column has been used"),
	CartesianProduct("cartesian product has been used"),
	Reference("Reference has been used");

	private final String description;
	NumberSource(String description)
	{
		this.description = description;
	}

	@Override
	public String toString()
	{
		return description;
	}
}
