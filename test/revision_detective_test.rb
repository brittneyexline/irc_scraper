require 'test/unit'
require File.dirname(__FILE__) + '/../conf/include'
require 'revision_detective.rb'

require 'time'

class RevisionDetectiveTest < Test::Unit::TestCase
  def setup
    @db = SQLite3::Database.new(":memory:")
    @db.execute('CREATE TABLE irc_wikimedia_org_en_wikipedia (
      id integer primary key autoincrement,
      article_name varchar(128) not null,
      desc varchar(8),
      revision_id integer,
      old_id integer,
      user varchar(64),
      byte_diff integer,
      ts timestamp(20),
      description text)')
    @clazz = RevisionDetective
    @detective = @clazz.new(@db)
    
    @info = [1, 'Amar Ben Belgacem', 'M', '392473902', '391225974', 'SD5', '+226', Time.parse('2010-02-10T22:17:39Z'), "fixes, added persondata, typos fixed: august 24 \342\206\222 August 24 using [[Project:AWB|AWB]]" ]

    @info2 = [2,
      'Vladimir Guerrero',
      'M',
      '392473934',
      '392337290',
      'Briskbaby',
      '+290',
      Time.parse('2010-02-10T22:17:39Z'),
      "fixes, added persondata, typos fixed: august 24 \342\206\222 August 24 using [[Project:AWB|AWB]]"
    ]
  end
  
  def test_find_revision_info
    #the url should be:
    #http://en.wikipedia.org/w/api.php?action=query&prop=revisions&revids=392473902&rvprop=ids|tags|flagged|timestamp|user|comment|size|flags|content
    revinfo = @detective.find_revision_info(@info)
    assert_equal([1287867544, 'SD5', "fixes, added persondata, typos fixed: august 24 \342\206\222 August 24 using [[Project:AWB|AWB]]", 6776, 1], [revinfo[0], revinfo[1], revinfo[2], revinfo[3], revinfo[5]])
  end

  def test_minor_info
    revinfo = @detective.find_revision_info(@info2)
    assert_equal([0], [revinfo[5]])
  end

  def test_link_info_links_added
    revinfo = @detective.find_revision_info(@info2)
    assert_equal([10], [revinfo[6]])
  end
  
  def test_link_info_no_links_added
    revinfo = @detective.find_revision_info(@info)
    assert_equal([0], [revinfo[6]])
  end

  def test_link_info_new_page
    assert_nothing_raised do
      revinfo = @detective.find_revision_info([
        3,
        'Category talk:Presidents of the Chamber of Deputies of Chile',
        'N',
        '404102956',
        '415922166',
        'Koavf',
        '+21',
        Time.parse('2010-02-10T22:17:39Z'),
        "tag using [[Project:AWB|AWB]]"
      ])
    end
  end

  def test_investigate
    @clazz.setup_table(@db)
    rownum = @detective.investigate(@info)
    assert_equal(1.to_s, rownum.to_s)
  end
  
  def test_setup_table
    #to test the sql of the table definition
    assert_nothing_raised do
      @clazz.setup_table(@db)
    end
  end
  
end