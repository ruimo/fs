import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './Site.css';
import MessagesLoader from "./MessagesLoader";
import cx from 'classnames';
import Calendar from 'react-calendar';

class Site extends Component {
  constructor(props) {
    super(props);
    this.state = {
      siteName: '',
      messages: MessagesLoader.Empty,
      page: 0,
      pageSize: 10,
      orderBy: 'site.created_at desc',
      records: [],
      deleteErrorMessage: '',
      date: new Date(),
      time: "00:00"
    };
  }
  
  onDateChanged = date => this.setState({date});

  renderRecords = async() => {
    try {
      const url = "/listSiteToUpdate?page=" + this.state.page
            + "&pageSize=" + this.state.pageSize
            + "&orderBySpec=" + this.state.orderBy;
      const resp = await fetch(encodeURI(url));

      if (resp.status === 200) {
        const json = await resp.json();
        console.log("json: " + JSON.stringify(json));
        this.setState({
          records: json['table']
        });
      } else if (resp.status === 401) {
        console.log("Login needed");
        this.props.history.push("/login/site")
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  componentDidMount = async() => {
    try {
      const resp = await fetch("/timeZoneInfo");

      if (resp.status === 200) {
        const json = await resp.json();
        const sel = document.getElementById('timeZoneSelect');
        const table = json['table'];
        for (let i = 0; i < table.length; ++i) {
          let opt = document.createElement('option');
          opt.value = i;
          opt.innerHTML = table[i];
          sel.appendChild(opt);
        }
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }

    this.renderRecords();

    try {
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'siteName'},
          { key: 'register'},
          { key: 'timeZone'},
          { key: 'dateTime'},
          { key: 'siteMaintenance'},
          { key: 'error.unknown'},
          { key: 'duplicated'},
          { key: 'owner'},
          { key: 'recordEmpty'},
          { key: 'deleteConfirm'},
          { key: 'delete'},
          { key: 'cancel'},
          { key: 'date'},
          { key: 'time'},
          { key: 'update'},
          { key: 'deleted'},
          { key: 'agentRecordWillBeDeleted'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  onCreateSite = () => {
    this.createOrUpdateSite("/createSite");
    this.setState({
      selectedSite: undefined
    });
  }

  onUpdateSite = () => {
    this.createOrUpdateSite("/updateSite?siteId=" + this.state.selectedSite.siteId);
  }

  createOrUpdateSite = async(url) => {
    try {
      this.setState({
        siteNameError: undefined,
        dateTimeError: undefined,
        globalError: undefined,
        message: undefined
      });

      const dateTime =
            this.state.date.getFullYear() + "/" +
            ('0' + (this.state.date.getMonth() + 1)).slice(-2) + "/" +
            ('0' + this.state.date.getDate()).slice(-2) +
             ' ' + this.state.time;

      const resp = await fetch(
        url,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Csrf-Token": "nocheck"
          },
          body: JSON.stringify({
            siteName: this.state.siteName,
            dateTime: dateTime,
            timeZoneIndex: document.getElementById('timeZoneSelect').selectedIndex,
          })
        }
      );

      console.log("status: " + resp.status);
      if (resp.status === 200) {
        this.setState({
          globalError: undefined,
          message: undefined,
        });
        this.renderRecords();
      } else if (resp.status === 400) {
        const json = await resp.json();
        console.log(JSON.stringify(json));
        this.setState({
          globalError: json[''],
          siteNameError: json['siteName'],
          dateTimeError: json['dateTime']
        });
      } else if (resp.status === 401) {
        this.props.history.push("/login/admin")
      } else if (resp.status === 403) {
        this.setState({
          globalError: this.msg('error.unknown')
        });
      } else if (resp.status === 404) {
        this.setState({
          globalError: this.msg('deleted')
        });
      } else if (resp.status === 409) {
        this.setState({
          siteNameError: [this.msg('duplicated')]
        });
      } else {
        this.setState({
          globalError: this.msg('error.unknown')
        });
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }
  
  msg = (key) => {
    return this.state.messages(key);
  }

  showDeleteDialog = (siteId, siteName) => {
    this.setState({
      deleteCandidate: {
        siteId: siteId,
        siteName: siteName
      }
    });
  }

  cancelDelete = (e) => {
    this.setState({
      deleteCandidate: undefined
    });
  }

  deleteSite = async(siteId) => {
    try {
      const resp = await fetch(
        "/deleteSite?siteId=" + siteId, {
          method: "POST",
          headers: {
            "Csrf-Token": "nocheck"
          },
          body: ''
        }
      );

      if (resp.status === 200) {
        this.setState({
          deleteCandidate: undefined
        });
        this.renderRecords();
      } else if (resp.status === 403) {
        this.setState({
          deleteErrorMessage: this.msg('unknownError')
        });
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  onSiteClicked = (siteId) => {
    this.props.history.push("/attend/" + siteId);
  }

  selectSite = (siteId, siteName, datetime, timezone) => {
    const dates = datetime.split(' ');
    const dateElems = dates[0].split('/');
    const date = new Date(dateElems[0], dateElems[1], dateElems[2]);
    const time = dates[1];

    this.setState({
      selectedSite: {
        siteId: siteId
      },
      siteName: siteName,
      date,
      time
    });

    const sel = document.getElementById('timeZoneSelect');
    const opts = sel.children;
    for (let i = 0; i < opts.length; ++i) {
      if (opts[i].innerHTML === timezone) {
        sel.value = i;
        break;
      }
    }
  }

  render() {
    const siteNameError = this.state.siteNameError !== undefined ?
      <div className="error">
        { this.state.siteNameError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";

    const dateTimeError = this.state.dateTimeError !== undefined ?
      <div className="error">
        { this.state.dateTimeError.map((e, i) => <div key={i}>{e}</div>) }
      </div>
      : "";

    const records = this.state.records.length === 0 ?
        <span className="emptyMessage">{this.msg('recordEmpty')}</span>
      :
        <table className="table is-striped">
          <thead>
            <tr>
              <th>{this.msg('siteName')}</th>
              <th>{this.msg('dateTime')}</th>
              <th>{this.msg('timeZone')}</th>
              <th>{this.msg('owner')}</th>
              <th></th>
            </tr>
          </thead>
          <tbody id="siteTable">
            {
              this.state.records.map((e) =>
                <tr key={e.siteId}
                    className={this.state.selectedSite !== undefined && this.state.selectedSite.siteId === e.siteId ? 'selected' : ''}
                    onClick={(ev) => this.selectSite(e.siteId, e.siteName, e.dateTime, e.timeZone)}>
                  <td>
                    {e.siteName}
                    &nbsp;
                    <a href="#attend" className="is-info button"
                       onClick={(ev) => this.onSiteClicked(e.siteId)}>
                      <i className="fas fa-external-link-alt"></i>
                    </a>
                  </td>
                  <td>{e.dateTime}</td>
                  <td>{e.timeZone}</td>
                  <td>{e.owner}</td>
                  <td>
                    <a href="#delete" className="is-danger button"
                       onClick={ev => this.showDeleteDialog(e.siteId, e.siteName)}>
                      <i className="far fa-trash-alt"></i>
                    </a>
                  </td>
                </tr>
              )
            }
          </tbody>
        </table>;

    return (
      <div className="site">
        { this.state.message !== undefined &&
          <article className="message is-info">
            <div className="message-body">{this.state.message}</div>
          </article>
        }

        { this.state.globalError !== undefined && 
          <article className="message is-danger">
            <div className="message-body">{this.state.globalError}</div>
          </article>
        }

        <nav className="panel">
          <p className="panel-heading">
            {this.msg('siteMaintenance')}
          </p>
          <div className="panel-block">
            <form>
              <div className="field">
                <div className="control">
                  <label className="label">{this.msg('siteName')}</label>
                  <div className="control">
                    <input className="input" type="text" placeholder={this.msg('siteName')} value={this.state.siteName}
                           onChange={(e) => this.setState({siteName: e.target.value})}
                    />
                  </div>
                  { siteNameError }
                </div>
              </div>

              <div className="field">
                <div className="control">
                  <label className="label">{this.msg('date')}</label>
                  <div className="control">
                    <Calendar onChange={this.onDateChanged} value={this.state.date}/>
                  </div>
                  { dateTimeError }
                </div>
              </div>

              <div className="field">
                <div className="control">
                  <label className="label">{this.msg('time')}</label>
                  <div className="control">
                    <input className="input" type="text" value={this.state.time}
                           onChange={(e) => this.setState({time: e.target.value})}/>
                  </div>
                  { dateTimeError }
                </div>
              </div>

              <div className="field">
                <div className="control">
                  <label className="label">{this.msg('timeZone')}</label>
                  <div className="select">
                    <select id="timeZoneSelect" name="timeZoneSelect">
                    </select>
                  </div>
                </div>
              </div>

              <div className="field">
                <p className="control">
                  { this.state.selectedSite !== undefined ?
                    <button type="button" className="button is-success update"
                            onClick={this.onUpdateSite}>{this.msg("update")}</button>
                    : ""}
                  <button type="button" className="button is-success"
                          onClick={this.onCreateSite}>{this.msg("register")}</button>
                </p>
              </div>
            </form>
          </div>
          <div className="panel-block">
            { records }
          </div>
        </nav>

        { /* Modals */ }
        <div className={cx("modal", {'is-active': this.state.deleteCandidate !== undefined})}>
          <div className="modal-background"></div>
          <div className="modal-content">
            <div className='dialogSiteName'>
              { this.state.deleteCandidate !== undefined ? this.state.deleteCandidate.siteName : "" }
            </div>
            <div>
              { this.msg('deleteConfirm') }
            </div>
            <div>
              { this.msg('agentRecordWillBeDeleted') }
            </div>
            <div className='dialogButtons'>
              <a href="#deleteSite" className="button is-danger"
                 onClick={(e) => {this.deleteSite(this.state.deleteCandidate.siteId);}}>
                {this.msg('delete')}
              </a>&nbsp;
              <a href="#deleteSite" className="button" onClick={(e) => {this.cancelDelete(e);}}>
                {this.msg('cancel')}
              </a>
            </div>
            <div className={cx("notification is-danger errorMessage", {'is-active': this.state.deleteErrorMessage !== ''})}>
              {this.state.deleteErrorMessage}
            </div>
          </div>
          <button className="modal-close is-large" aria-label="close" onClick={(e) => {this.cancelDelete(e);}}></button>
        </div>
      </div>
    );
  }
}

export default withRouter(Site);
